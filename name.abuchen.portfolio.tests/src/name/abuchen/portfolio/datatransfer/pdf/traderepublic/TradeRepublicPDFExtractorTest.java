package name.abuchen.portfolio.datatransfer.pdf.traderepublic;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
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
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.io.IOException;
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
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;

@SuppressWarnings("nls")
public class TradeRepublicPDFExtractorTest
{
    TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client())
    {
        @Override
        protected CoinGeckoQuoteFeed lookupFeed()
        {
            // mock the list of coins to avoid remote call
            return new CoinGeckoQuoteFeed()
            {
                @Override
                public synchronized List<Coin> getCoins() throws IOException
                {
                    return List.of( //
                                    new Coin("bitcoin", "BTC", "Bitcoin"), //
                                    new Coin("ethereum", "ETH", "Ethereum"));
                }
            };
        }
    };

    @Test
    public void testWertpapierKauf01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BKM4GZ66"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-05-13T12:14")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Order: dead-beef | Ausführung: ab12-c3de"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.05))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.05))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US08862E1091"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Beyond Meat Inc. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-05-13T13:59")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Order: 1234-abcd | Ausführung: a1b2-3456"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(59.19))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(58.19))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US88160R1014"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Tesla Inc. Registered Shares DL-,001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-06-17T12:27")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Order: fd98-0283 | Ausführung: 51cb-50a8"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(193.08))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(192.08))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007472060"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Wirecard AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-05T15:16")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertThat(entry.getNote(), is("Order: a1b2-3456 | Ausführung: 1234-56bb"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(485.80))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(484.80))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US7561091049"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Realty Income Corp. Registered Shares DL 1"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-21T15:57")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Kauf05.txt"));
        assertThat(entry.getNote(), is("Order: xxxx-xxxx | Ausführung: xxxx-xxxx"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1396.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1395.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US88579Y1010"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("3M Co. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-02-15T17:39")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7)));
        assertThat(entry.getSource(), is("Kauf06.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(976.80))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(975.80))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000BASF111"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("BASF SE Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-05-02T21:26")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getSource(), is("Kauf07.txt"));
        assertThat(entry.getNote(), is("Order: 01f6-b7cc | Ausführung: a952-e304"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(95.69))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(94.69))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("GB0007980591"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("BP PLC Registered Shares DL -,25"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-07-14T16:48")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
        assertThat(entry.getSource(), is("Kauf08.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(855.90))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(854.90))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-02-09T00:00"), hasShares(0.09351), //
                        hasSource("Kauf12.txt"), //
                        hasNote("Ausführung: 8f44-fdf5 | Round Up: 42c2-50a7"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1829220216"), hasWkn(null), hasTicker(null), //
                        hasName("MUL Amundi MSCI AC World UCITS ETF Inh.Anteile Acc"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-04T00:00"), hasShares(0.032743), //
                        hasSource("Kauf13.txt"), //
                        hasNote("Ausführung: 5437-f7f5 | Saveback: B2C4-n64q"), //
                        hasAmount("EUR", 13.78), hasGrossValue("EUR", 13.78), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf14()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
    public void testBuy01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000SQ6QKU9"), hasWkn(null), hasTicker(null), //
                        hasName("Société Générale Effekten GmbH MiniL O.End CBOE VIX 11,49"), //
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
    public void testAcquisto01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Acquisto01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
    public void testCryptoKauf01()
    {
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
                        hasNote("Ausführung: K7Y2-2e37 | Sparplan: y646-a753"), //
                        hasAmount("EUR", 24.99), hasGrossValue("EUR", 24.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testCryptoKauf03()
    {
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf04.txt"), errors);

        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoKauf05.txt"), errors);

        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
    public void testCryptoVerkauf01()
    {
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CryptoVerkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Achat01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US88032Q1094"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Tencent Holdings Ltd. Reg.Sh.(unsp.ADRs)/1 HD -,0001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-01-17T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.3773)));
        assertThat(entry.getSource(), is("Achat01.txt"));
        assertThat(entry.getNote(), is("Exécution : cee1-2d00 | Programmé : eea2-4c8b"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testKontoauszug01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(5L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-11T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertNull(transaction.getNote());
    }

    @Test
    public void testKontoauszug02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(299.96))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertNull(transaction.getNote());
    }

    @Test
    public void testKontoauszug03()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.20))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertNull(transaction.getNote());
    }

    @Test
    public void testKontoauszug04()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-13T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(123.45))));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-28T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(123.45))));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertNull(transaction.getNote());

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(123.45))));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertNull(transaction.getNote());
    }

    @Test
    public void testKontoauszug05()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        assertThat(results, hasItem(interest(hasDate("2023-04-01"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-05-01"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-06-01"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));
    }

    @Test
    public void testSteuerabrechnung01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Steuerabrechnung01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.75 + 0.21 + 0.30))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.75 + 0.21 + 0.30))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSteuerabrechnung02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerabrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
    public void testSteuerkorrektur01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerkorrektur01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US92936U1097"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("W.P. Carey Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-11-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(38.4597)));
        assertThat(transaction.getSource(), is("Steuerkorrektur01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.30))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.30))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("AU000000CUV3"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Clinuvel Pharmaceuticals Ltd. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-06-18T17:50")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(80)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Order: 55a8-39ad | Ausführung: 051a-e65e"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1792.29))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1825.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30.63 + 1.68))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000TR95XU9"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HSBC Trinkaus & Burkhardt AG Call 15.12.21 DAX 14000"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-10T11:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(500)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Order: 1234-1234 | Ausführung: 1234-1234"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3594.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3595.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-10T11:42")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(500)));
        assertThat(transaction.getSource(), is("Verkauf02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.63))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.63))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007100000"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Daimler AG Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-21T09:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));
        assertThat(entry.getSource(), is("Verkauf03.txt"));
        assertThat(entry.getNote(), is("Order: c17d-baea | Ausführung: 6415-fd77"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1199.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-21T09:30")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(30)));
        assertThat(transaction.getSource(), is("Verkauf03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(139.58))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(139.58))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B0M62Q58"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-12T12:19")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.0068)));
        assertThat(entry.getSource(), is("Verkauf04.txt"));
        assertThat(entry.getNote(), is("Order: 36de-8883 | Ausführung: f439-3735"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.29))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.29))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A3H2333"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HAMBORNER REIT AG Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-26T11:44")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.3632)));
        assertThat(entry.getSource(), is("Verkauf05.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.17))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.17))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A3H23V7"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Epigenomics AG Inhaber-Bezugsrechte"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-25T11:32")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(16)));
        assertThat(entry.getSource(), is("Verkauf06.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.34))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.34))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check fee transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-25T11:32")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(16)));
        assertThat(transaction.getSource(), is("Verkauf06.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf07()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0006070006"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HOCHTIEF AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-23T21:10")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(9)));
        assertThat(entry.getSource(), is("Verkauf07.txt"));
        assertThat(entry.getNote(), is("Order: 5a93-03d1 | Ausführung: b11c-12c4"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(623.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(624.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-23T21:10")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(9)));
        assertThat(transaction.getSource(), is("Verkauf07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.56 + 0.25))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.56 + 0.25))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf08()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US6700024010"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Novavax Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-03T16:49")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getSource(), is("Verkauf08.txt"));
        assertThat(entry.getNote(), is("Order: 886a-ffff | Ausführung: dcd2-ffff"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(301.88))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(302.88))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-03T16:49")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(2)));
        assertThat(transaction.getSource(), is("Verkauf08.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.65 + 0.69))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.65 + 0.69))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testTilgung01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tilgung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000TT22GS8"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HSBC Trinkaus & Burkhardt AG TurboC O.End Linde"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(700)));
        assertThat(entry.getSource(), is("Tilgung01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.70))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.70))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(700)));
        assertThat(transaction.getSource(), is("Tilgung01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(29.24 + 1.61 + 2.34))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(29.24 + 1.61 + 2.34))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testTilgung02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tilgung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE1234567891"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HSBC Trinkaus & Burkhardt AG TurboP O.End ShopApEu"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(250)));
        assertThat(entry.getSource(), is("Tilgung02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.25))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.25))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividendeStorno01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        Security security = new Security("Realty Income Corp. Registered Shares DL 1", CurrencyUnit.EUR);
        security.setIsin("US7561091049");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2020-02-26T00:00"), hasShares(200), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 30.17), hasGrossValue("EUR", 40.61), //
                                        hasTaxes("EUR", ((6.81 / 1.1105) + 4.09 + 0.22)), hasFees("EUR", 0.00), //
                                        check(tx -> {
                                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                                            Account account = new Account();
                                            account.setCurrencyCode(CurrencyUnit.EUR);
                                            Status s = c.process((AccountTransaction) tx, account);
                                            assertThat(s, is(Status.OK_STATUS));
                                        })))));

        // check tax refund transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        taxRefund( //
                                        hasDate("2020-02-26T00:00"), hasShares(200), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.36), hasGrossValue("EUR", 0.36), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                                        check(tx -> {
                                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                                            Account account = new Account();
                                            account.setCurrencyCode(CurrencyUnit.EUR);
                                            Status s = c.process((AccountTransaction) tx, account);
                                            assertThat(s, is(Status.OK_STATUS));
                                        })))));
    }

    @Test
    public void testDividendeStorno02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US36162J1060"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("GEO Group Inc., The Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check cancellation (Storno) transaction
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((AccountTransaction) cancellation.getSubject()).getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorOrderCancellationUnsupported));

        assertThat(((Transaction) cancellation.getSubject()).getDateTime(), is(LocalDateTime.parse("2021-05-25T00:00")));
        assertThat(((Transaction) cancellation.getSubject()).getShares(), is(Values.Share.factorize(79)));
        assertThat(((Transaction) cancellation.getSubject()).getSource(), is("DividendeStorno02.txt"));
        assertNull(((Transaction) cancellation.getSubject()).getNote());

        assertThat(((Transaction) cancellation.getSubject()).getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(29.66))));
        assertThat(((Transaction) cancellation.getSubject()).getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(34.90))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.24))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = ((Transaction) cancellation.getSubject()).getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(37.92))));
    }

    @Test
    public void testDividende01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B652H904"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShsV-EM Dividend UCITS ETF Registered Shares USD o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-25T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.18))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.11))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.89 + 0.04))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5.63))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        Security security = new Security("iShsV-EM Dividend UCITS ETF Registered Shares USD o.N.", CurrencyUnit.EUR);
        security.setIsin("IE00B652H904");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-25T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.18))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.11))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.89 + 0.04))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US5949181045"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Microsoft Corp. Registered Shares DL-,00000625"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.10))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.16))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.62 + 0.42 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.60))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        Security security = new Security("Microsoft Corp. Registered Shares DL-,00000625", CurrencyUnit.EUR);
        security.setIsin("US5949181045");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.10))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.16))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.62 + 0.42 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende03()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US09247X1019"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Blackrock Inc. Reg. Shares Class A DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.20))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.96))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.45 + 0.28 + 0.01 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(3.30))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        Security security = new Security("Blackrock Inc. Reg. Shares Class A DL -,01", CurrencyUnit.EUR);
        security.setIsin("US09247X1019");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.20))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.96))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.45 + 0.28 + 0.01 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende04()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0F5UH1"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.63))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.63))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende05()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US8754651060"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Tanger Fact.Outlet Centrs Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-02-14T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(142)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(39.21))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(46.13))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.92))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(50.41))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        Security security = new Security("Tanger Fact.Outlet Centrs Inc. Registered Shares DL -,01", CurrencyUnit.EUR);
        security.setIsin("US8754651060");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-02-14T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(142)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(39.21))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(46.13))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.92))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende06()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("GB00B03MLX29"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Royal Dutch Shell Reg. Shares Class A EO -,07"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-21T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(500)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.30))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(67.75))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.15 + 6.90 + 0.40))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(80.00))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        Security security = new Security("Royal Dutch Shell Reg. Shares Class A EO -,07", CurrencyUnit.EUR);
        security.setIsin("GB00B03MLX29");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-21T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(500)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.30))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(67.75))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.15 + 6.90 + 0.40))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende07()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US9314271084"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Walgreens Boots Alliance Inc. Reg. Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-11T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3.3272)));
        assertThat(transaction.getSource(), is("Dividende07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.09))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.28))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.56))));
    }

    @Test
    public void testDividende07WithSecurityInEUR()
    {
        Security security = new Security("Walgreens Boots Alliance Inc. Reg. Shares DL -,01", CurrencyUnit.EUR);
        security.setIsin("US9314271084");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-11T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3.3272)));
        assertThat(transaction.getSource(), is("Dividende07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.09))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.28))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende08()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B1FZS574"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShsII-MSCI Turkey UCITS ETF Registered Shs USD (Dist) o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-26T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(124.0903)));
        assertThat(transaction.getSource(), is("Dividende08.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.61))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(33.43))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.36 + 0.46))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(41.04))));
    }

    @Test
    public void testDividende08WithSecurityInEUR()
    {
        Security security = new Security("iShsII-MSCI Turkey UCITS ETF Registered Shs USD (Dist) o.N.", CurrencyUnit.EUR);
        security.setIsin("IE00B1FZS574");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-26T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(124.0903)));
        assertThat(transaction.getSource(), is("Dividende08.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.61))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(33.43))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.36 + 0.46))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende09()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005785604"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Fresenius SE & Co. KGaA Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(4.3521)));
        assertThat(transaction.getSource(), is("Dividende09.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.83))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.83))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.95 + 0.05))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende10()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FI0009005961"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Stora Enso Oyj Reg. Shares Cl.R EO 1,70"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(25)));
        assertThat(transaction.getSource(), is("Dividende10.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.20))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.75))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.13 + 0.37 + 0.02 + 0.03))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende11()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FI0009005987"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("UPM Kymmene Corp. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-16T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(50)));
        assertThat(transaction.getSource(), is("Dividende11.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(45.50))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(65.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19.50))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende12()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("GB00BH4HKS39"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Vodafone Group PLC Registered Shares DL 0,2095238"));
        assertThat(security.getCurrencyCode(), is("GBP"));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-06T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(699)));
        assertThat(transaction.getSource(), is("Dividende12.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.43))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.43))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("GBP", Values.Amount.factorize(26.80))));

        // check for the reinvestment of dividends
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22)));
        assertThat(entry.getPortfolioTransaction().getSource(), is("Dividende12.txt"));
        assertNull(entry.getPortfolioTransaction().getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende12WithSecurityInEUR()
    {
        Security security = new Security("Vodafone Group PLC Registered Shares DL 0,2095238", CurrencyUnit.EUR);
        security.setIsin("GB00BH4HKS39");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-06T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(699)));
        assertThat(transaction.getSource(), is("Dividende12.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.43))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.43))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check for the reinvestment of dividends
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22)));
        assertThat(entry.getPortfolioTransaction().getSource(), is("Dividende12.txt"));
        assertNull(entry.getPortfolioTransaction().getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende13()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BKBF6H24"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShsIII-Core MSCI World U.ETF Reg. Shares EUR Hgd (Dis) o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende13.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.92))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.35))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.41 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende14()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US7960542030"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Samsung SDI Co. Ltd. Reg.Shs(Sp.GDRs 144A)/4 SW5000"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-04-21T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(6.214744)));
        assertThat(transaction.getSource(), is("Dividende14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.76))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.23 / 1.0985))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.27 / 1.0958))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.12 / 1.0958))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.23))));
    }

    @Test
    public void testDividende14WithSecurityInEUR()
    {
        Security security = new Security("Samsung SDI Co. Ltd. Reg.Shs(Sp.GDRs 144A)/4 SW5000", CurrencyUnit.EUR);
        security.setIsin("US7960542030");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-04-21T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(6.214744)));
        assertThat(transaction.getSource(), is("Dividende14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.76))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.23 / 1.0985))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.27 / 1.0958))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.12 / 1.0958))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende15()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("Apple Inc. Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-16T00:00"), hasShares(0.0929), //
                        hasSource("Dividende15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasForexGrossValue("USD", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15WithSecurityInEUR()
    {
        Security security = new Security("Apple Inc. Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("US0378331005");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-16T00:00"), hasShares(0.0929), //
                        hasSource("Dividende15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende16()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        Security security = new Security("Philip Morris Internat. Inc. Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("US7181721090");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-10T00:00"), hasShares(55), //
                        hasSource("Dividende16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 63.31), hasGrossValue("EUR", 63.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-01-10T00:00"), hasShares(55), //
                        hasSource("Dividende16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.43), hasGrossValue("EUR", 5.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividendo01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividendo01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        Security security = new Security("Enbridge Inc. Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("CA29250N1050");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividendo01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-06-01T00:00"), hasShares(20.971565), //
                        hasSource("Dividendo01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.56), hasGrossValue("EUR", 12.74), //
                        hasTaxes("EUR", (4.65 / 1.46055)), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividendo02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividendo02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE001A0F8UH1"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-07-17T00:00"), hasShares(23.632607), //
                        hasSource("Dividendo02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.64), hasGrossValue("EUR", 16.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKaptialreduktion01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kapitalreduktion01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CA0679011084"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Barrick Gold Corp. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check capital reduction transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8.4226)));
        assertThat(transaction.getSource(), is("Kapitalreduktion01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.71))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.97))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.25 + 0.01))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.18))));
    }

    @Test
    public void testKaptialreduktion01WithSecurityInEUR()
    {
        Security security = new Security("Barrick Gold Corp. Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("CA0679011084");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kapitalreduktion01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8.4226)));
        assertThat(transaction.getSource(), is("Kapitalreduktion01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.71))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.97))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.25 + 0.01))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testKapitalerhoehungGegenBar01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KapitalerhoehungGegenBar01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotuebertragEingehend.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SpinOff01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umtausch01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umtausch02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BKM4GZ66"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(173.3905)));
        assertThat(transaction.getSource(), is("Vorabpauschale01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.30 + 0.02))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.30 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
    public void testSparplan01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B4L5Y983"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-18T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.4534)));
        assertThat(entry.getSource(), is("Sparplan01.txt"));
        assertThat(entry.getNote(), is("Ausführung: ff2f-1254"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSparplan02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0H0744"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iSh.DJ Asia Pa.S.D.30 U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.9261)));
        assertThat(entry.getSource(), is("Sparplan02.txt"));
        assertThat(entry.getNote(), is("Ausführung: xxxx-xxxx | Sparplan: xxxx-xxxx"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSparplan03()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE0031442068"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShs Core S&P 500 UC.ETF USDD Registered Shares USD (Dist)oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-18T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5.5520)));
        assertThat(entry.getSource(), is("Sparplan03.txt"));
        assertThat(entry.getNote(), is("Ausführung: 8eac-25ab | Sparplan: 77c8-1b0c"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(150.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(150.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSparplan04()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0000121014"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("LVMH Moët Henn. L. Vuitton SE Actions Port. (C.R.) EO 0,3"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-18T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.0028)));
        assertThat(entry.getSource(), is("Sparplan04.txt"));
        assertThat(entry.getNote(), is("Ausführung: 3609-6874 | Sparplan: 98a0-1d15"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1003.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSparplan05()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("NL0010273215"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ASML Holding N.V. Aandelen op naam EO -,09"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-04-19T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.0358)));
        assertThat(entry.getSource(), is("Sparplan05.txt"));
        assertThat(entry.getNote(), is("Ausführung: fc69-6fc3 | Sparplan: af97-c4b1"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19.96))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19.96))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSparplan06()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0846707026"), hasWkn(null), hasTicker(null), //
                        hasName("Berkshire Hathaway Inc. Reg.Shares B New DL -,00333"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-05-16T00:00"), hasShares(0.3367), //
                        hasSource("Sparplan06.txt"), //
                        hasNote("Execution: e083-506a | Savings plan: 7687-2574"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplan07()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0846707026"), hasWkn(null), hasTicker(null), //
                        hasName("Berkshire Hathaway Inc. Reg.Shares B New DL -,00333"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-02T00:00"), hasShares(0.081967), //
                        hasSource("Sparplan07.txt"), //
                        hasNote("Execution: d008-0f58 | Savings plan: dbc9-ad4d"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplan08()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFMXXD54"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard S&P 500 UCITS ETF Reg. Shs USD Acc. oN"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-02T00:00"), hasShares(0.336491), //
                        hasSource("Sparplan08.txt"), //
                        hasNote("Execution: 8c26-15e1 | Savings plan: 6af7-5be3"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFusion01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fusion01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Split01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zwangsuebernahme01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        Security security = new Security("Quantafuel AS Navne-Aksjer NK -,01", CurrencyUnit.EUR);
        security.setIsin("NO0010785967");

        Client client = new Client();
        client.addSecurity(security);

        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zwangsuebernahme01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-02-12T00:00"), hasShares(17.00), //
                        hasSource("Zwangsuebernahme01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.54), hasGrossValue("EUR", 9.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSteuerlicherUmtausch01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerlicherUmtausch01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerlicherUmtausch02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vergleichsverfahren01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Titelumtausch01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

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
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Einzahlungsabrechnung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check deposit transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2024-01-07T00:00"), //
                        hasSource("Einzahlungsabrechnung01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 49.00), hasGrossValue("EUR", 49.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-01-31T00:00"), //
                        hasSource("Zinsabrechnung01.txt"), //
                        hasNote("2,00%"), //
                        hasAmount("EUR", 0.88), hasGrossValue("EUR", 0.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-01-31T00:00"), //
                        hasSource("Zinsabrechnung02.txt"), //
                        hasNote("2,00%"), //
                        hasAmount("EUR", 2.58), hasGrossValue("EUR", 2.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZinsabrechnung03()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabrechnung03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-10-31T00:00"), //
                        hasSource("Zinsabrechnung03.txt"), //
                        hasNote("01.10.2023 - 31.10.2023 (4,00%)"), //
                        hasAmount("EUR", 112.40), hasGrossValue("EUR", 152.67), //
                        hasTaxes("EUR", 38.17 + 2.10), hasFees("EUR", 0.00))));
    }

    @Test
    public void testRescontoInteressiMaturati01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "RescontoInteressiMaturati01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-05-31"), //
                        hasSource("RescontoInteressiMaturati01.txt"), //
                        hasNote("2,00%"), //
                        hasAmount("EUR", 0.12), hasGrossValue("EUR", 0.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testInterestInvoice01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "InterestInvoice01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-09-30"), //
                        hasSource("InterestInvoice01.txt"), //
                        hasNote("2,00%"), //
                        hasAmount("EUR", 1.47), hasGrossValue("EUR", 1.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
