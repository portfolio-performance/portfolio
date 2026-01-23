package name.abuchen.portfolio.datatransfer.pdf.postfinance;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
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

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.PostfinancePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.TestCoinSearchProvider;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;

@SuppressWarnings("nls")
public class PostfinancePDFExtractorTest
{
    PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(new Client())
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
        var client = new Client();

        var extractor = new PostfinancePDFExtractor(client);

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
        assertThat(security.getIsin(), is("NL0000009355"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("UNILEVER DUTCH CERT"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-09-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(60)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Referenz: 153557048"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(2850.24))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(2837.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(4.26))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(8.58))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var client = new Client();

        var extractor = new PostfinancePDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0025751329"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("LOGITECH N"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-10-25T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(87)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Referenz: 155571206"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(2998.95))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(2970.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(2.25))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(25.00 + 1.50))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var client = new Client();

        var extractor = new PostfinancePDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000PAH0038"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("PORSCHE AUTOMOBIL HOLDING PRF"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-27T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(55)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Referenz: 122007844"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(2968.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(2963.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(4.45))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.60 * 1.08279))));

        var grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("EUR", Values.Amount.factorize(2736.80))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInCHF()
    {
        var security = new Security("PORSCHE AUTOMOBIL HOLDING PRF", "CHF");
        security.setIsin("DE000PAH0038");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-27T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(55)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Referenz: 122007844"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(2968.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(2963.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(4.45))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.60 * 1.08279))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("CHF");
        var s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var client = new Client();

        var extractor = new PostfinancePDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0188802960"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Pictet - Japan Index - I"));
        assertThat(security.getCurrencyCode(), is("JPY"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-12-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.441)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertThat(entry.getNote(), is("Auftrag 10111111"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(280.91))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(280.49))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.42))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        var grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("JPY", Values.Amount.factorize(34019.00))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInCHF()
    {
        var security = new Security("Pictet - Japan Index - I", "CHF");
        security.setIsin("LU0188802960");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-12-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.441)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertThat(entry.getNote(), is("Auftrag 10111111"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(280.91))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(280.49))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.42))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("CHF");
        var s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0014933193"), hasWkn(null), hasTicker(null), //
                        hasName("PF - Global Fund A"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-31T00:00"), hasShares(1.216), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Auftrag 15186945"), //
                        hasAmount("CHF", 200.00), hasGrossValue("CHF", 198.02), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.98))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0105994401"), hasWkn(null), hasTicker(null), //
                        hasName("UBS ETF - SXI Real Estate Funds"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-14T00:00"), hasShares(14.000), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Auftrag 19888922"), //
                        hasAmount("CHF", 141.28), hasGrossValue("CHF", 141.17), //
                        hasTaxes("CHF", 0.11), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var client = new Client();

        var extractor = new PostfinancePDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BD4TYL27"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("UBSETF MSCI USA hdg to CHF"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-09-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(308)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Referenz: 153327044"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(7467.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(7481.30))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(11.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(2.60))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0482006191"), hasWkn(null), hasTicker(null), //
                        hasName("UBS Index Fund - Bonds"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-16T00:00"), hasShares(0.074), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftrag 21187458"), //
                        hasAmount("CHF", 71.14), hasGrossValue("CHF", 71.14), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0309035367"), hasWkn(null), hasTicker(null), //
                        hasName("Pictet - Short-Term Money Market"), //
                        hasCurrencyCode("JPY"))));

        // check dividends transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-18T00:00"), hasShares(0.553), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftrag 21796086"), //
                        hasAmount("CHF", 310.79), hasGrossValue("CHF", 310.79), //
                        hasForexGrossValue("JPY", 55384.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testtestWertpapierVerkauf03WithSecurityInCHF()
    {
        var security = new Security("Pictet - Short-Term Money Market", "CHF");
        security.setIsin("LU0309035367");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-18T00:00"), hasShares(0.553), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftrag 21796086"), //
                        hasAmount("CHF", 310.79), hasGrossValue("CHF", 310.79), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("USD"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-01T00:00"), hasShares(0.006124), //
                        hasSource("CryptoKauf01.txt"), //
                        hasNote("Auftrag 37463178"), //
                        hasAmount("CHF", 538.73), hasGrossValue("CHF", 533.66), //
                        hasForexGrossValue("USD", 596.33), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 5.07))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("NL0000009355"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("UNILEVER DUTCH CERT"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-05T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(60)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertThat(transaction.getNote(), is("Referenz: 169933304"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(20.93))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(24.62))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(3.69))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0032912732"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("UBS ETF CH - SLI CHF A"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-09-08T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(34)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertThat(transaction.getNote(), is("Referenz: 130516418"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(36.69))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(56.44))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(19.75))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0019396990"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("YPSOMED HLDG"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-07-05T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(19)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertThat(transaction.getNote(), is("Referenz: 149619136"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(26.60))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(26.60))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testZahlungsverkehr01()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zahlungsverkehr01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-02"), hasAmount("CHF", 1200.00), //
                        hasSource("Zahlungsverkehr01.txt"), hasNote("Referenz: 391377700"))));
    }

    @Test
    public void testZahlungsverkehr02()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zahlungsverkehr02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-01-31"), hasAmount("CHF", 5675.50), //
                        hasSource("Zahlungsverkehr02.txt"), hasNote("Referenz: 119387423"))));
    }

    @Test
    public void testJahresgebuehr01()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Jahresgebuehr01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check fees transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-01-03T00:00")));
        assertThat(transaction.getSource(), is("Jahresgebuehr01.txt"));
        assertThat(transaction.getNote(), is("Jahresgebühr | Referenz: 161333839"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(90.00))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(90.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testGiroKontoauszug10()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotgebuehr01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-01-01"), hasAmount("CHF", 18.00), //
                        hasSource("Depotgebuehr01.txt"), hasNote("Depotgebühr | Referenz: 464973467"))));
    }

    @Test
    public void testZinsabschluss01()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabschluss01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check 1st interest transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-10-31T00:00")));
        assertThat(transaction.getSource(), is("Zinsabschluss01.txt"));
        assertThat(transaction.getNote(), is("01.01.2019 – 31.10.2019"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(4.17))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(4.17))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 2nd interest transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-31T00:00")));
        assertThat(transaction.getSource(), is("Zinsabschluss01.txt"));
        assertThat(transaction.getNote(), is("01.11.2019 – 31.12.2019"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(0.42))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(0.42))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testZinsabschluss02()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabschluss02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "CHF");

        // check 1st interest transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-10-31T00:00")));
        assertThat(transaction.getSource(), is("Zinsabschluss02.txt"));
        assertThat(transaction.getNote(), is("01.01.2019 – 31.10.2019"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(83.33))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(83.33))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 2nd interest transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-31T00:00")));
        assertThat(transaction.getSource(), is("Zinsabschluss02.txt"));
        assertThat(transaction.getNote(), is("01.11.2019 – 31.12.2019"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(33.33))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(33.33))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check taxes transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-31T00:00")));
        assertThat(transaction.getSource(), is("Zinsabschluss02.txt"));
        assertThat(transaction.getNote(), is("Verrechnungssteuer 01.01.2019 - 31.12.2019"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(40.83))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(40.83))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testZinsabschluss03()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabschluss03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check interest transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-12-31T00:00")));
        assertThat(transaction.getSource(), is("Zinsabschluss03.txt"));
        assertThat(transaction.getNote(), is("01.01.2015 - 31.12.2015"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1.00))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(1.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check fees transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-12-31T00:00")));
        assertThat(transaction.getSource(), is("Zinsabschluss03.txt"));
        assertThat(transaction.getNote(), is("Gebührenausweis 01.01.2015 - 31.12.2015"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(60.00))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(60.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testZinsabschluss04()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zinsabschluss04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "CHF");

        // check interest transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-12-31T00:00")));
        assertThat(transaction.getSource(), is("Zinsabschluss04.txt"));
        assertThat(transaction.getNote(), is("01.01.2017 - 31.12.2017"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(400.00))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(400.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check taxes transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-12-31T00:00")));
        assertThat(transaction.getSource(), is("Zinsabschluss04.txt"));
        assertThat(transaction.getNote(), is("Verrechnungssteuer 01.01.2017 - 31.12.2017"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(140.00))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(140.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check fees transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-12-31T00:00")));
        assertThat(transaction.getSource(), is("Zinsabschluss04.txt"));
        assertThat(transaction.getNote(), is("Gebührenausweis 01.01.2017 - 31.12.2017"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(60.00))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(60.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("CHF", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(76L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(76));
        new AssertImportActions().check(results, "CHF");

        // check transaction
        // get transactions
        var iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(76L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Auftrag CH-DD-Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Auftrag CH-DD-Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Auftrag CH-DD-Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Oranger Einzahlungsschein"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Oranger Einzahlungsschein"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Oranger Einzahlungsschein"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(25.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Dienstleistung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(15.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Dienstleistung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Dienstleistung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(2000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Übertrag auf Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Übertrag auf Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(500.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Übertrag auf Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1300.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Giro Post"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Giro Post"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(200.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Giro Post"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Giro Bank"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Giro Bank"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(200.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Giro Bank"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-07T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-07T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(15.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-07T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(25.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-07T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Bargeldbezug vom 07.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-07T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Bargeldbezug vom 07.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-07T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Bargeldbezug vom 07.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Bargeldbezug"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Bargeldbezug"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Bargeldbezug"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Dienstleistung vom 08.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Dienstleistung vom 08.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Dienstleistung vom 08.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Dienstleistung vom 08.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Dienstleistung vom 08.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Dienstleistung vom 08.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping vom 09.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(15.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping vom 09.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(25.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping vom 09.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping vom 09.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(15.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping vom 09.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(25.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping vom 09.04.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Geld senden"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(15.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Geld senden"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(25.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Geld senden"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Geld senden"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(15.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Geld senden"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(25.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Geld senden"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Kauf/Dienstleistung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(15.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Kauf/Dienstleistung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(25.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Kauf/Dienstleistung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Geld empfangen"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(15.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Geld empfangen"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(25.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Geld empfangen"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-11T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(11.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Geld empfangen"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-11T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(16.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Geld empfangen"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-11T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(26.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("TWINT Geld empfangen"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-20T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutschrift von Fremdbank"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-20T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(600.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutschrift von Fremdbank"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-20T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(500.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutschrift von Fremdbank"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-21T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Einzahlschein/QR-Zahlteil"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-21T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Einzahlschein/QR-Zahlteil"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-21T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Einzahlschein/QR-Zahlteil"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutschrift Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutschrift Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutschrift Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-23T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-23T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-23T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(11.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is(""));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is(""));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(9.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is(""));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(5.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kontoführungsgebühr"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(2.40))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Einzahlungen am Schalter"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(15.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Guthabengebühr für 03.2018"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1.00))));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Zinsabschluss 01.01.2018 - 30.04.2018"));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "CHF");

        // check transaction
        // get transactions
        var iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(6L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(20000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Übertrag auf Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(20000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Übertrag auf Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(40000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Übertrag auf Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(25000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Übertrag aus Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-11T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(25000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Übertrag aus Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-13T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(10000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Übertrag aus Konto"));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(54L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(54));
        new AssertImportActions().check(results, "CHF");

        // check transaction
        // get transactions
        var iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(54L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("E-FINANCE 01-22222-5"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("E-FINANCE 01-22222-5"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(350.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("E-FINANCE 01-22222-5"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Auftrag DEBIT DIRECT"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Auftrag DEBIT DIRECT"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Auftrag DEBIT DIRECT"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(200.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-07T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(200.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kauf/Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Bargeldbezug"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Bargeldbezug"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Bargeldbezug"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-11T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Online-Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(75.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Online-Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(25.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Online-Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-13T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kauf/Dienstleistung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-14T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(50.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kauf/Dienstleistung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-14T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kauf/Dienstleistung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Auftrag CH-DD-Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-16T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(200.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Auftrag CH-DD-Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-16T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(300.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Auftrag CH-DD-Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Giro Internation (SEPA)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-18T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Giro Internation (SEPA)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-18T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Giro Internation (SEPA)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Übertrag auf Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Übertrag auf Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Giro Ausland"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Giro Ausland"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Giro Ausland"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1300.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Giro aus Online-SIC"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(2000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Giro aus Online-SIC"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Giro aus Online-SIC"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-23T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Giro aus Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-23T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(500.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Giro aus Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-23T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Giro aus Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-24T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Gutschrift Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-24T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(500.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Gutschrift Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-24T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Gutschrift Online Shopping"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Gutschrift von Fremdbank"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(500.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Gutschrift von Fremdbank"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Gutschrift von Fremdbank"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-26T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Gutschrift von Fremdbank"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-26T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(500.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Gutschrift von Fremdbank"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-26T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(250.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Gutschrift von Fremdbank"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(150.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Übertrag aus Konto"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Überweisungsgebühr (SEPA)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(2.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kontoführungsgebühr"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(5.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kontoführungsgebühr"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(4.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Jahrespreis Login"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(24.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kontoführungsgebühr (Papier)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Zinsabschluss 01.04.2015 - 30.04.2015"));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check transaction
        // get transactions
        var iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(2L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(111.00))));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(11.00))));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05MitTwintKauf.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(27L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(27));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-11-27"), //
                        hasAmount("CHF", 13.90), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 27.11.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-02"), //
                        hasAmount("CHF", 24.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 02.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-08"), //
                        hasAmount("CHF", 3.20), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 08.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-09"), //
                        hasAmount("CHF", 3.20), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 09.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-09"), //
                        hasAmount("CHF", 20.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 09.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-09"), //
                        hasAmount("CHF", 5.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Online Shopping"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-10"), //
                        hasAmount("CHF", 13.80), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 10.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-11"), //
                        hasAmount("CHF", 16.50), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 11.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-11"), //
                        hasAmount("CHF", 18.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 11.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-12"), //
                        hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 12.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-13"), //
                        hasAmount("CHF", 8.20), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 13.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-13"), //
                        hasAmount("CHF", 20.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 13.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-14"), //
                        hasAmount("CHF", 28.85), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 14.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-12"), //
                        hasAmount("CHF", 8.80), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 12.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-15"), //
                        hasAmount("CHF", 21.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("TWINT Kauf/Dienstleistung"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-16"), //
                        hasAmount("CHF", 26.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 16.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-16"), //
                        hasAmount("CHF", 5.70), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 16.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-17"), //
                        hasAmount("CHF", 25.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 17.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2024-12-20"), //
                        hasAmount("CHF", 6465.80), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-24"), //
                        hasAmount("CHF", 2000.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-27"), //
                        hasAmount("CHF", 5056.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-27"), //
                        hasAmount("CHF", 2000.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-27"), //
                        hasAmount("CHF", 1720.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Lastschrift"))));

        // assert transaction
        // NOTE: This is an 'inboundDelivery' from the savings account instead
        // of a 'deposit', but finding the accompanying 'outboundDelivery' in
        // the savings accounts bank statement might be impossible
        assertThat(results, hasItem(deposit( //
                        hasDate("2024-12-27"), //
                        hasAmount("CHF", 2500.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Übertrag aus Konto CH0000000000000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-28"), //
                        hasAmount("CHF", 12.31), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Kauf/Dienstleistung vom 28.12.2024"))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-12-30"), //
                        hasAmount("CHF", 4.72), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote("Depotgebühr"))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-12-31"), //
                        hasAmount("CHF", 5.00), //
                        hasSource("Kontoauszug05MitTwintKauf.txt"), //
                        hasNote(""))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new PostfinancePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2025-01-04"), hasAmount("CHF", 70.30), //
                        hasSource("Kontoauszug06.txt"), hasNote("Kauf/Dienstleistung"))));
    }
}
