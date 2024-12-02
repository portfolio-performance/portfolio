package name.abuchen.portfolio.datatransfer.pdf.baaderbank;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import name.abuchen.portfolio.datatransfer.pdf.BaaderBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TestCoinSearchProvider;
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
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;

@SuppressWarnings("nls")
public class BaaderBankPDFExtractorTest
{
    BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client())
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
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE0032895942"));
        assertThat(security.getWkn(), is("911950"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShs DL Corp Bond UCITS ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-20T15:31:10")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 1234567"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(208.95))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(208.74))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.21))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A1C22M3"));
        assertThat(security.getWkn(), is("A1C22M"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HSBC S&P 500 UCITS ETF Bearer Shares (Dt. Zert.) o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-20T14:59:17")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(70)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 1234567"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1551.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1549.45))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.55))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0489337690"));
        assertThat(security.getWkn(), is("DBX0F1"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Xtr.FTSE Devel.Europ.R.Estate Inhaber-Anteile 1C o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-22T16:14:40")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 123456789"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(87.30))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(87.30))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0677077884"));
        assertThat(security.getWkn(), is("DBX0MB"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Xtr.II USD Emerging Markets Bd Inhaber-Anteile 2D USD o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-12-10T12:58:54")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 12999999"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(28.18))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(28.18))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B3XXRP09"));
        assertThat(security.getWkn(), is("A1JX53"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Vanguard S&P 500 UCITS ETF Registered Shares USD o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-12-21T12:45:34")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(24)));
        assertThat(entry.getSource(), is("Kauf05.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 3814982"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1030.25))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1030.25))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("ES0173093024"));
        assertThat(security.getWkn(), is("A2ANA3"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Red Electrica Corporacion S.A. Acciones Port. EO -,50"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-24T14:49:46")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(70)));
        assertThat(entry.getSource(), is("Kauf06.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: x"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(985.12))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(983.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.97))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("JP3436100006"));
        assertThat(security.getWkn(), is("891624"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("SoftBank Group Corp. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-12T18:00:10")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(13)));
        assertThat(entry.getSource(), is("Kauf07.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 137111111"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(519.03))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(519.03))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US5021751020"));
        assertThat(security.getWkn(), is("884625"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("LTC Properties Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-29T15:49:21")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(12)));
        assertThat(entry.getSource(), is("Kauf08.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 13924926"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(513.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(513.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0908500753"));
        assertThat(security.getWkn(), is("LYX0Q0"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Lyxor Index-L.Co.St.EO 600(DR) Actions Nom.UCITS ETF Acc oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-09-06T12:39:33")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.118)));
        assertThat(entry.getSource(), is("Kauf09.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 11111111"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.57))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.57))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A3H3MG0"));
        assertThat(security.getWkn(), is("A3H3MG"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Enapter AG junge Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-03-09T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(46)));
        assertThat(entry.getSource(), is("Kauf10.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: yyyyyyy | Verhältnis: 1 : 1"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1012.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1012.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B4L5Y983"));
        assertThat(security.getWkn(), is("A0RPWH"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-02-28T13:48:52")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.734)));
        assertThat(entry.getSource(), is("Kauf11.txt"));
        assertThat(entry.getNote(), is("Transaction No.: 65879250"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(199.93))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(199.93))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CA2553311002"));
        assertThat(security.getWkn(), is("A12C65"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Diversified Royalty Corp. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-04-05T16:54:29")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1000)));
        assertThat(entry.getSource(), is("Kauf12.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 94196810"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2440.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2440.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BFYN8Y92"));
        assertThat(security.getWkn(), is("A2JR0G"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HANetf-EMQQ Em.Mkts Int.+Ecom. Reg.Shs EUR Acc. o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-04-29T09:32:39")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11)));
        assertThat(entry.getSource(), is("Kauf13.txt"));
        assertThat(entry.getNote(), is("Transaction No.: 204751222"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(94.05))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(94.05))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf14()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US0378331005"));
        assertThat(security.getWkn(), is("865985"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Apple Inc. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-01-27T16:25:24")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6)));
        assertThat(entry.getSource(), is("Kauf14.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 12350123"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(878.04))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(878.04))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf15()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf15.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BQZJBM26"));
        assertThat(security.getWkn(), is("A12HUS"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("WisdomTree E.Mkts SC Div.U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-01-02T11:34:01")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.309)));
        assertThat(entry.getSource(), is("Kauf15.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 156968932"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.99))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.99))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf16()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf16.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BTN1Y115"));
        assertThat(security.getWkn(), is("A14M2J"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Medtronic PLC Registered Shares DL -,0001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-02-01T13:11:03")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.325)));
        assertThat(entry.getSource(), is("Kauf16.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 164499999"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.96))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.96))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf17()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf17.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B3XXRP09"));
        assertThat(security.getWkn(), is("A1JX53"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Vanguard S&P 500 UCITS ETF Registered Shares USD Dis.oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-04-11T12:23:20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.699)));
        assertThat(entry.getSource(), is("Kauf17.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 4712"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf18()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf18.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US1912161007"));
        assertThat(security.getWkn(), is("850663"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Coca-Cola Co., The Registered Shares DL -,25"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-04-11T12:04:45")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.024)));
        assertThat(entry.getSource(), is("Kauf18.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 184899171"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.38))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.38))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf19()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf19.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0F5UH1"));
        assertThat(security.getWkn(), is("A0F5UH"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-04-11T12:30:45")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.804)));
        assertThat(entry.getSource(), is("Kauf19.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 184697063"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf20()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf20.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0WMPJ6"));
        assertThat(security.getWkn(), is("A0WMPJ"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("AIXTRON SE Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-04-12T09:00:06")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));
        assertThat(entry.getSource(), is("Kauf20.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.:"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1539.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1539.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf21()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf21.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US94106L1098"));
        assertThat(security.getWkn(), is("893579"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Waste Management Inc. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-04-11T12:31:19")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.10)));
        assertThat(entry.getSource(), is("Kauf21.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 184714818"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.94))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.94))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf22()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf22.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US5007541064"));
        assertThat(security.getWkn(), is("A14TU4"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Kraft Heinz Co., The Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-04-14T14:04:19")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
        assertThat(entry.getSource(), is("Kauf22.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 00000000"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(357.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(357.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf23()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000GP0Q523"), hasWkn("GP0Q52"), hasTicker(null), //
                        hasName("Goldman Sachs Bank Europe SE TuBear O.End Nasd100 14200"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-04-20T20:28:12"), hasShares(110), //
                        hasSource("Kauf23.txt"), //
                        hasNote("Vorgangs-Nr.: 12345678"), //
                        hasAmount("EUR", 1042.04), hasGrossValue("EUR", 1041.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierKauf24()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88579Y1010"), hasWkn("851745"), hasTicker(null), //
                        hasName("3M Co. Registered Shares DL -,01"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-05-04T11:34:56"), hasShares(0.268), //
                        hasSource("Kauf24.txt"), //
                        hasNote("Vorgangs-Nr.: 002052907"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf25()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf25.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFNM3P36"), hasWkn("A2N6TH"), hasTicker(null), //
                        hasName("iShs IV-iShs MSCI EMIMI ES ETF Reg. Shares USD Acc. o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-04-26T10:49:40"), hasShares(4.235), //
                        hasSource("Kauf25.txt"), //
                        hasNote("Vorgangs-Nr.: 1801"), //
                        hasAmount("EUR", 21.45), hasGrossValue("EUR", 21.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf26()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf26.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AU000000BHP4"), hasWkn("850524"), hasTicker(null), //
                        hasName("BHP Group Ltd. Registered Shares DL -,50"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-04-19T11:55:01"), hasShares(1.881), //
                        hasSource("Kauf26.txt"), //
                        hasNote("Vorgangs-Nr.: 186694373"), //
                        hasAmount("EUR", 53.98), hasGrossValue("EUR", 53.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf27()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf27.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("BMG4069C1486"), hasWkn("879151"), hasTicker(null), //
                        hasName("Great Eagle Holdings Ltd. Registered Shares HD -,50"), //
                        hasCurrencyCode("HKD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-14T08:48:32"), hasShares(52.22), //
                        hasSource("Kauf27.txt"), //
                        hasNote("Vorgangs-Nr.: 123456"), //
                        hasAmount("EUR", 91.89), hasGrossValue("EUR", 91.77), hasForexGrossValue("HKD", 775.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.12))));
    }

    @Test
    public void testWertpapierKauf27WithSecurityInEUR()
    {
        Security security = new Security("Great Eagle Holdings Ltd. Registered Shares HD -,50", CurrencyUnit.EUR);
        security.setIsin("BMG4069C1486");
        security.setWkn("879151");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf27.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-14T08:48:32"), hasShares(52.22), //
                        hasSource("Kauf27.txt"), //
                        hasNote("Vorgangs-Nr.: 123456"), //
                        hasAmount("EUR", 91.89), hasGrossValue("EUR", 91.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.12), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf28()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US6541061031"), hasWkn("866993"), hasTicker(null), //
                        hasName("NIKE Inc. Registered Shares Class B o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-01T15:25:32"), hasShares(12.00), //
                        hasSource("Kauf28.txt"), //
                        hasNote("Vorgangs-Nr.: 997010271"), //
                        hasAmount("EUR", 1151.76), hasGrossValue("EUR", 1151.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf29()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf29.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0130293662"), hasWkn("A1JLZG"), hasTicker(null), //
                        hasName("BKW AG Namens-Aktien SF 2,50"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-06T10:13:44"), hasShares(5.00), //
                        hasSource("Kauf29.txt"), //
                        hasNote("Vorgangs-Nr.: 123456789"), //
                        hasAmount("EUR", 813.38), hasGrossValue("EUR", 804.17), //
                        hasForexGrossValue("CHF", 772.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.21 + 4.00))));
    }

    @Test
    public void testWertpapierKauf29WithSecurityInEUR()
    {
        Security security = new Security("BKW AG Namens-Aktien SF 2,50", CurrencyUnit.EUR);
        security.setIsin("CH0130293662");
        security.setWkn("A1JLZG");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf29.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-06T10:13:44"), hasShares(5.00), //
                        hasSource("Kauf29.txt"), //
                        hasNote("Vorgangs-Nr.: 123456789"), //
                        hasAmount("EUR", 813.38), hasGrossValue("EUR", 804.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.21 + 4.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf30()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf30.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000VU9QLB0"), hasWkn("VU9QLB"), hasTicker(null), //
                        hasName("Vontobel Financial Products Aktienanleihe v.23(24)ZAL"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-14T11:24:19"), hasShares(10.00), //
                        hasSource("Kauf30.txt"), //
                        hasNote("Vorgangs-Nr.: 248368163 | Stückzinsen für 123 Tag(e): 51,25 EUR"), //
                        hasAmount("EUR", 1024.05), hasGrossValue("EUR", 1024.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf31()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf31.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US91086QAS75"), hasWkn("A0DDHM"), hasTicker(null), //
                        hasName("Mexiko DL-Medium-Term Notes 2004(34)"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-17T10:37:30"), hasShares(10.00), //
                        hasSource("Kauf31.txt"), //
                        hasNote("Vorgangs-Nr.: 249453219 | Stückzinsen für 54 Tag(e): 9,36 EUR"), //
                        hasAmount("EUR", 970.27), hasGrossValue("EUR", 970.27), //
                        hasForexGrossValue("USD", 1050.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf31WithSecurityInEUR()
    {
        Security security = new Security("Mexiko DL-Medium-Term Notes 2004(34)", CurrencyUnit.EUR);
        security.setIsin("US91086QAS75");
        security.setWkn("A0DDHM");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf31.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-17T10:37:30"), hasShares(10.00), //
                        hasSource("Kauf31.txt"), //
                        hasNote("Vorgangs-Nr.: 249453219 | Stückzinsen für 54 Tag(e): 9,36 EUR"), //
                        hasAmount("EUR", 970.27), hasGrossValue("EUR", 970.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf32()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf32.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH1134540470"), hasWkn("A3C20K"), hasTicker(null), //
                        hasName("On Holding AG Namens-Aktien A SF-,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-06T19:25:22"), hasShares(100.00), //
                        hasSource("Kauf32.txt"), //
                        hasNote("Vorgangs-Nr.: 275613522"), //
                        hasAmount("EUR", 2621.66), hasGrossValue("EUR", 2613.71), //
                        hasForexGrossValue("USD", 2810.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.95 + 4.00))));
    }

    @Test
    public void testWertpapierKauf32WithSecurityInEUR()
    {
        Security security = new Security("On Holding AG Namens-Aktien A SF-,01", CurrencyUnit.EUR);
        security.setIsin("CH1134540470");
        security.setWkn("A3C20K");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf32.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-06T19:25:22"), hasShares(100.00), //
                        hasSource("Kauf32.txt"), //
                        hasNote("Vorgangs-Nr.: 275613522"), //
                        hasAmount("EUR", 2621.66), hasGrossValue("EUR", 2613.71), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.95 + 4.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf33()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf33.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0290358497"), hasWkn("DBX0AN"), hasTicker(null), //
                        hasName("Xtrackers II EUR Over.Rate Sw. Inhaber-Anteile 1C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-01-04T08:28:27"), hasShares(79.00), //
                        hasSource("Kauf33.txt"), //
                        hasNote("Vorgangs-Nr.: 123456789"), //
                        hasAmount("EUR", 11043.90), hasGrossValue("EUR", 11030.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 13.13))));
    }

    @Test
    public void testWertpapierKauf34()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf34.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0001141802"), hasWkn("114180"), hasTicker(null), //
                        hasName("Bundesrep.Deutschland Bundesobl.Ser.180 v.2019(24)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-18T15:50:39"), hasShares(508.00), //
                        hasSource("Kauf34.txt"), //
                        hasNote("Vorgangs-Nr.: 138151585"), //
                        hasAmount("EUR", 49922.68), hasGrossValue("EUR", 49922.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf35()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf35.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B60SX394"), hasWkn("A0RGCS"), hasTicker(null), //
                        hasName("InvescoMI MSCI World ETF Registered Shares Acc o.N"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-11T16:50:57"), hasShares(275.00), //
                        hasSource("Kauf35.txt"), //
                        hasNote("Vorgangs-Nr.: 248513926"), //
                        hasAmount("EUR", 28323.63), hasGrossValue("EUR", 28323.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0446734526"));
        assertThat(security.getWkn(), is("A0X97T"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("UBS-ETF-UBS-ETF MSCI Pa.(ExJ.) Inhaber-Anteile A o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-05-10T14:10:26")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 1234567"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(75.92))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(76.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.08))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE0032895942"));
        assertThat(security.getWkn(), is("911950"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShs DL Corp Bond UCITS ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-19T11:13:08")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 01234567"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1161.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1161.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0446734526"));
        assertThat(security.getWkn(), is("A0X97T"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("UBS-ETF-UBS-ETF MSCI PXJ U.ETF Inhaber-Anteile (USD) A-dis oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-12-21T13:06:55")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6)));
        assertThat(entry.getSource(), is("Verkauf03.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 3800004"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(226.68))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(226.68))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0TGJ55"));
        assertThat(security.getWkn(), is("A0TGJ5"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("VARTA AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-13T14:56:06")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
        assertThat(entry.getSource(), is("Verkauf04.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 01234567"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(809.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(809.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US30212P3038"));
        assertThat(security.getWkn(), is("A1JRLJ"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Expedia Group Inc. Registered Shares DL-,0001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-30T16:34:43")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Verkauf05.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 22222222"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1454.56))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1600.80))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(127.73 + 11.49 + 7.02))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CA55378N1078"));
        assertThat(security.getWkn(), is("120504"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("MTY Food Group Inc. Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is("CAD"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-10-05T16:03:24")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.20)));
        assertThat(entry.getSource(), is("Verkauf06.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 11111111"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.85))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.23 + 0.01 + 0.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CAD", Values.Amount.factorize(12.92))));
    }

    @Test
    public void testWertpapierVerkauf06WithSecurityInEUR()
    {
        Security security = new Security("MTY Food Group Inc. Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("CA55378N1078");
        security.setWkn("120504");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-10-05T16:03:24")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.20)));
        assertThat(entry.getSource(), is("Verkauf06.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 11111111"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.85))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.23 + 0.01 + 0.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testWertpapierVerkauf07()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US75513E1010"));
        assertThat(security.getWkn(), is("A2PZ0R"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Raytheon Technologies Corp. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.339)));
        assertThat(entry.getSource(), is("Verkauf07.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 11 | Spitzenregulierung"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.03))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.03))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf08()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000TT410K7"));
        assertThat(security.getWkn(), is("TT410K"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("HSBC Trinkaus & Burkhardt AG Call 14.06.23 S&P500 5000"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-05-20T21:57:22")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1520)));
        assertThat(entry.getSource(), is("Verkauf08.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 11111111"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(562.40))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(562.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf09()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BJ5JP105"));
        assertThat(security.getWkn(), is("A2PHCF"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShs V-MSCI W.En.Sec.U.ETF Reg. Shs USD Dis. oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-07-11T15:49:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(59.365)));
        assertThat(entry.getSource(), is("Verkauf09.txt"));
        assertThat(entry.getNote(), is("Transaction No.: 204751222"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(329.36))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(329.36))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf10()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0010524777"));
        assertThat(security.getWkn(), is("LYX0CB"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Lyxor MSCI NE ESG Fil.DR UETF Actions au Port.Dist o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-07-29T14:03:29")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.54)));
        assertThat(entry.getSource(), is("Verkauf10.txt"));
        assertThat(entry.getNote(), is("Transaction No.: 204751222"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(106.59))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(106.59))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf11()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BMW3QX54"));
        assertThat(security.getWkn(), is("A12DB1"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("L&G-L&G R.Gbl Robot.Autom.UETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-12-29T10:25:26")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(121.853)));
        assertThat(entry.getSource(), is("Verkauf11.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 156268888"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2093.43))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2093.43))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf12()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BYXG2H39"));
        assertThat(security.getWkn(), is("A2DWAW"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShsIV-NASDAQ US Biotech.U.ETF Registered Shares USD Acc.o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-12-01T09:04:37")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(157.563)));
        assertThat(entry.getSource(), is("Verkauf12.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 12345768"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(945.38))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(945.38))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf13()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BZ163K21"));
        assertThat(security.getWkn(), is("A143JM"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Vanguard USD Corporate B.U.ETF Registered Shares USD Dis.oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-12-01T14:03:19")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        assertThat(entry.getSource(), is("Verkauf13.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 12345768"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(180.82))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(180.82))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf14()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BYWQWR46"));
        assertThat(security.getWkn(), is("A2PLDF"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("VanEck Vid eSports UC. ETF Reg. Shares A USD Acc. o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-12-29T10:11:56")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25.03)));
        assertThat(entry.getSource(), is("Verkauf14.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 123456"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(635.51))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(635.51))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf15()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf15.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BYZK4552"));
        assertThat(security.getWkn(), is("A2ANH0"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShsIV-Automation&Robot.U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-12-29T10:22:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(183.347)));
        assertThat(entry.getSource(), is("Verkauf15.txt"));
        assertThat(entry.getNote(), is("Vorgangs-Nr.: 123456"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1602.45))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1602.45))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf16()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000VU354U9"), hasWkn("VU354U"), hasTicker(null), //
                        hasName("Vontobel Financial Products DIZ 22.12.23 LVMH 700"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-12-22T00:00"), hasShares(3), //
                        hasSource("Verkauf16.txt"), //
                        hasNote("Vorgangs-Nr.: 77777888"), //
                        hasAmount("EUR", 2088.13), hasGrossValue("EUR", 2100.00), //
                        hasTaxes("EUR", 11.26 + 0.61), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf17()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0290358497"), hasWkn("DBX0AN"), hasTicker(null), //
                        hasName("Xtrackers II EUR Over.Rate Sw. Inhaber-Anteile 1C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-01-04T11:54:40"), hasShares(20), //
                        hasSource("Verkauf17.txt"), //
                        hasNote("Vorgangs-Nr.: 123456789"), //
                        hasAmount("EUR", 2785.40), hasGrossValue("EUR", 2792.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.00 + 3.32))));
    }

    @Test
    public void testWertpapierVerkauf18()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf18.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000HB2KBG9"), hasWkn("HB2KBG"), hasTicker(null), //
                        hasName("UniCredit Bank AG HVB Inline 18.05.22 BASF 45-70"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-05-25T00:00"), hasShares(6), //
                        hasSource("Verkauf18.txt"), //
                        hasNote("Vorgangs-Nr.: 8679954 | Ablauf der Optionsfrist"), //
                        hasAmount("EUR", 60.00), hasGrossValue("EUR", 60.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf19()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf19.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US60468T1051"), hasWkn("A1W1XV"), hasTicker(null), //
                        hasName("Mirati Therapeutics Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-01-25T00:00"), hasShares(4), //
                        hasSource("Verkauf19.txt"), //
                        hasNote("Vorgangs-Nr.: 25742277 | Obligatorische Barabfindung"), //
                        hasAmount("EUR", 212.68), hasGrossValue("EUR", 212.68), //
                        hasForexGrossValue("USD", 232.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf19WithSecurityInEUR()
    {
        Security security = new Security("Mirati Therapeutics Inc. Registered Shares DL -,01", CurrencyUnit.EUR);
        security.setIsin("US60468T1051");
        security.setWkn("A1W1XV");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf19.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-01-25T00:00"), hasShares(4), //
                        hasSource("Verkauf19.txt"), //
                        hasNote("Vorgangs-Nr.: 25742277 | Obligatorische Barabfindung"), //
                        hasAmount("EUR", 212.68), hasGrossValue("EUR", 212.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf20()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0001104875"), hasWkn("110487"), hasTicker(null), //
                        hasName("Bundesrep.Deutschland Bundesschatzanw. v.22(24)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-15T00:00"), hasShares(2080), //
                        hasSource("Verkauf20.txt"), //
                        hasNote("Vorgangs-Nr.: 06501068"), //
                        hasAmount("EUR", 206626.75), hasGrossValue("EUR", 208000.00), //
                        hasTaxes("EUR", 1301.66 + 71.59), hasFees("EUR", 0.00))));
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
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"), //
                        hasFeed(CoinGeckoQuoteFeed.ID), //
                        hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-01-02T11:04:16"), hasShares(0.0024), //
                        hasSource("CryptoKauf01.txt"), //
                        hasNote("Vorgangs-Nr.: KRY 000000123456"), //
                        hasAmount("EUR", 101.75), hasGrossValue("EUR", 100.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testVorabpauschale01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BWBXM385"));
        assertThat(security.getWkn(), is("A14QBZ"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("SPDR S+P US Con.Sta.Sel.S.UETF"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check tax transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(112)));
        assertThat(transaction.getSource(), is("Vorabpauschale01.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: yyyyyyy | Zahlungszeitraum: 01.01.2020 - 31.12.2020"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.04))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.04))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

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
                        hasIsin("IE00BD1F4L37"), hasWkn("A2AP34"), hasTicker(null), //
                        hasName("iShsIV-Edge MSCI USA Q.F.U.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2024-01-02T00:00"), hasShares(220), //
                                        hasSource("Vorabpauschale02.txt"), //
                                        hasNote("Vorgangs-Nr.: $Vorgangsnummer | Zahlungszeitraum: 01.01.2023 - 31.12.2023"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testAdvanceTax01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AdvanceTax01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKM4GZ66"), hasWkn("A111X9"), hasTicker(null), //
                        hasName("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-01-02T00:00"), hasShares(418), //
                        hasSource("AdvanceTax01.txt"), //
                        hasNote("Transaction No.: 12484826 | Payment Period: 2023-01-01 - 2023-12-31"), //
                        hasAmount("EUR", 3.12), hasGrossValue("EUR", 3.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0000130577"));
        assertThat(security.getWkn(), is("859386"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Publicis Groupe S.A."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-06T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(57)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 5555555"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(68.22))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(114.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.60 + 1.22 + 0.75 + 30.21))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005933931"));
        assertThat(security.getWkn(), is("593393"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShares Core DAX UCITS ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends tax transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-05-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(11)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 123456"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.24))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.24))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende03()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B2NPKV68"));
        assertThat(security.getWkn(), is("A0NECU"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShsII-J.P.M.$ EM Bond U.ETF Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 123456"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.08))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.44))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.32 + 0.02 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(3.94))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        Security security = new Security("iShsII-J.P.M.$ EM Bond U.ETF Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("IE00B2NPKV68");
        security.setWkn("A0NECU");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-30T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(8)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 123456"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.08))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.44))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.32 + 0.02 + 0.02))));
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
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A14KRD3"));
        assertThat(security.getWkn(), is("A14KRD"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Deutsche Konsum REIT-AG"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-16T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(35)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 1111111"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.00))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende05()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US57636Q1040"));
        assertThat(security.getWkn(), is("A0F602"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Mastercard Inc."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-08T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 1111111111"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.84))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.11))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.10 + 0.17))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.20))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        Security security = new Security("Mastercard Inc.", CurrencyUnit.EUR);
        security.setIsin("US57636Q1040");
        security.setWkn("A0F602");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-08T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(3)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 1111111111"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.84))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.11))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.10 + 0.17))));
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
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US56035L1044"));
        assertThat(security.getWkn(), is("A0X8Y3"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Main Street Capital Corp."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(23)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 1111111"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.23))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.36))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.41 + 0.03 + 0.03 + 0.66))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.72))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        Security security = new Security("Main Street Capital Corp.", CurrencyUnit.EUR);
        security.setIsin("US56035L1044");
        security.setWkn("A0X8Y3");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-05-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(23)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 1111111"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.23))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.36))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.41 + 0.03 + 0.03 + 0.66))));
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
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US92936U1097"));
        assertThat(security.getWkn(), is("A1J5SB"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("W.P. Carey Inc."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(14)));
        assertThat(transaction.getSource(), is("Dividende07.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 5555555"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.98))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.40))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.34 + 0.07 + 2.01))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(14.56))));
    }

    @Test
    public void testDividende07WithSecurityInEUR()
    {
        Security security = new Security("W.P. Carey Inc.", CurrencyUnit.EUR);
        security.setIsin("US92936U1097");
        security.setWkn("A1J5SB");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(14)));
        assertThat(transaction.getSource(), is("Dividende07.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 5555555"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.98))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.40))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.34 + 0.07 + 2.01))));
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
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A2G9LL1"));
        assertThat(security.getWkn(), is("A2G9LL"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Deutsche Industrie REIT-AG"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(62)));
        assertThat(transaction.getSource(), is("Dividende08.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 5566777"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.72))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.88))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.64 + 0.32 + 0.20))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende09()
    {
        /**
         * We know that there is still an delivery in-/outbound exit here, but
         * we don't know how to post it.
         */
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005557508"));
        assertThat(security.getWkn(), is("555750"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Deutsche Telekom AG"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-24T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(77)));
        assertThat(transaction.getSource(), is("Dividende09.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 222222"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(46.20))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(46.20))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende10()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BYSX4739"));
        assertThat(security.getWkn(), is("A2PQDR"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("F.UC.-Fid.Em.Mkt.Qual.In.U.ETF Reg. Shs USD Dis. oN"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-25T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(7.367)));
        assertThat(transaction.getSource(), is("Dividende10.txt"));
        assertThat(transaction.getNote(), is("Transaction No.: 204751222"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.20))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.20))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.22))));
    }

    @Test
    public void testDividende10WithSecurityInEUR()
    {
        Security security = new Security("F.UC.-Fid.Em.Mkt.Qual.In.U.ETF Reg. Shs USD Dis. oN", CurrencyUnit.EUR);
        security.setIsin("IE00BYSX4739");
        security.setWkn("A2PQDR");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-25T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(7.367)));
        assertThat(transaction.getSource(), is("Dividende10.txt"));
        assertThat(transaction.getNote(), is("Transaction No.: 204751222"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.20))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.20))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende11()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0H0744"));
        assertThat(security.getWkn(), is("A0H074"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iSh.DJ Asia Pa.S.D.50 U.ETF DE"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1.225)));
        assertThat(transaction.getSource(), is("Dividende11.txt"));
        assertThat(transaction.getNote(), is("Transaction No.: 204751222"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende12()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US1101221083"));
        assertThat(security.getWkn(), is("850501"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Bristol-Myers Squibb Co."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-02-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(30)));
        assertThat(transaction.getSource(), is("Dividende12.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 11111111"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.57))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.67))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.52 + 0.14 + 0.08 + 2.36))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(17.10))));
    }

    @Test
    public void testDividende12WithSecurityInEUR()
    {
        Security security = new Security("Bristol-Myers Squibb Co.", CurrencyUnit.EUR);
        security.setIsin("US1101221083");
        security.setWkn("850501");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-02-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(30)));
        assertThat(transaction.getSource(), is("Dividende12.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 11111111"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.57))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.67))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.52 + 0.14 + 0.08 + 2.36))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
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
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US09075V1026"));
        assertThat(security.getWkn(), is("A2PSR2"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("BioNTech SE Nam.-Akt.(sp.ADRs)1/o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getSource(), is("Dividende13.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.00))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00 + 1.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(30.00))));
    }

    @Test
    public void testDividende13WithSecurityInEUR()
    {
        Security security = new Security("BioNTech SE Nam.-Akt.(sp.ADRs)1/o.N.", CurrencyUnit.EUR);
        security.setIsin("US09075V1026");
        security.setWkn("A2PSR2");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getSource(), is("Dividende13.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.00))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00 + 1.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende14()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005557508"));
        assertThat(security.getWkn(), is("555750"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Deutsche Telekom AG"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-04-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(13.629)));
        assertThat(transaction.getSource(), is("Dividende14.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 00000000"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.54))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.54))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende15()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

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
                        hasIsin("US0378331005"), hasWkn("865985"), hasTicker(null), //
                        hasName("Apple Inc."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-02-16T00:00"), hasShares(2.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Transaction No.: 13241804"), //
                        hasAmount("EUR", 0.26), hasGrossValue("EUR", 0.43), //
                        hasForexGrossValue("USD", 0.46), //
                        hasTaxes("EUR", 0.03 + 0.14), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15WithSecurityInEUR()
    {
        Security security = new Security("Apple Inc.", CurrencyUnit.EUR);
        security.setIsin("US0378331005");
        security.setWkn("865985");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

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
                        hasDate("2023-02-16T00:00"), hasShares(2.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Transaction No.: 13241804"), //
                        hasAmount("EUR", 0.26), hasGrossValue("EUR", 0.43), //
                        hasTaxes("EUR", 0.03 + 0.14), hasFees("EUR", 0.00), //
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
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn("865985"), hasTicker(null), //
                        hasName("Apple Inc."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-05-18T00:00"), hasShares(2.00), //
                        hasSource("Dividende16.txt"), //
                        hasNote("Transaction No.: 15390292"), //
                        hasAmount("EUR", 0.32), hasGrossValue("EUR", 0.44), //
                        hasForexGrossValue("USD", 0.48), //
                        hasTaxes("EUR", 0.12), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende16WithSecurityInEUR()
    {
        Security security = new Security("Apple Inc.", CurrencyUnit.EUR);
        security.setIsin("US0378331005");
        security.setWkn("865985");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-05-18T00:00"), hasShares(2.00), //
                        hasSource("Dividende16.txt"), //
                        hasNote("Transaction No.: 15390292"), //
                        hasAmount("EUR", 0.32), hasGrossValue("EUR", 0.44), //
                        hasTaxes("EUR", 0.12), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende17()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn("865985"), hasTicker(null), //
                        hasName("Apple Inc."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-16T00:00"), hasShares(60.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Vorgangs-Nr.: 20120279"), //
                        hasAmount("USD", 12.24), hasGrossValue("USD", 14.40), //
                        hasTaxes("USD", 2.16), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende17WithSecurityInEUR()
    {
        Security security = new Security("Apple Inc.", CurrencyUnit.EUR);
        security.setIsin("US0378331005");
        security.setWkn("865985");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-16T00:00"), hasShares(60.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Vorgangs-Nr.: 20120279"), //
                        hasAmount("USD", 12.24), hasGrossValue("USD", 14.40), //
                        hasForexGrossValue("EUR", 13.24), //
                        hasTaxes("USD", 2.16), hasFees("USD", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.USD);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende18()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende18.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn("870747"), hasTicker(null), //
                        hasName("Microsoft Corp."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-14T00:00"), hasShares(5.00), //
                        hasSource("Dividende18.txt"), //
                        hasNote("Vorgangs-Nr.: 123"), //
                        hasAmount("USD", 2.79), hasGrossValue("USD", 3.75), //
                        hasTaxes("USD", 0.38 + 0.02 + 0.56), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende18WithSecurityInEUR()
    {
        Security security = new Security("Microsoft Corp.", CurrencyUnit.EUR);
        security.setIsin("US5949181045");
        security.setWkn("870747");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende18.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-14T00:00"), hasShares(5.00), //
                        hasSource("Dividende18.txt"), //
                        hasNote("Vorgangs-Nr.: 123"), //
                        hasAmount("USD", 2.79), hasGrossValue("USD", 3.75), //
                        hasForexGrossValue("EUR", 3.43), //
                        hasTaxes("USD", 0.38 + 0.02 + 0.56), hasFees("USD", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.USD);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende19()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende19.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B1XNHC34"), hasWkn("A0MW0M"), hasTicker(null), //
                        hasName("iShsII-Gl.Clean Energy U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-29T00:00"), hasShares(140.00), //
                        hasSource("Dividende19.txt"), //
                        hasNote("Vorgangs-Nr.: 20483837"), //
                        hasAmount("USD", 6.26), hasGrossValue("USD", 6.26), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende19WithSecurityInEUR()
    {
        Security security = new Security("iShsII-Gl.Clean Energy U.ETF Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("IE00B1XNHC34");
        security.setWkn("A0MW0M");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende19.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-11-29T00:00"), hasShares(140.00), //
                        hasSource("Dividende19.txt"), //
                        hasNote("Vorgangs-Nr.: 20483837"), //
                        hasAmount("USD", 6.26), hasGrossValue("USD", 6.26), //
                        hasForexGrossValue("EUR", 5.70), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.USD);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende20()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US09247X1019"), hasWkn("928193"), hasTicker(null), //
                        hasName("Blackrock Inc."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-22T00:00"), hasShares(4.00), //
                        hasSource("Dividende20.txt"), //
                        hasNote("Vorgangs-Nr.: XXX"), //
                        hasAmount("USD", 14.89), hasGrossValue("USD", 20.00), //
                        hasTaxes("USD", 2.00 + 0.11 + 3.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende20WithSecurityInEUR()
    {
        Security security = new Security("Blackrock Inc.", CurrencyUnit.EUR);
        security.setIsin("US09247X1019");
        security.setWkn("928193");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-22T00:00"), hasShares(4.00), //
                        hasSource("Dividende20.txt"), //
                        hasNote("Vorgangs-Nr.: XXX"), //
                        hasAmount("USD", 14.89), hasGrossValue("USD", 20.00), //
                        hasForexGrossValue("EUR", 17.98), //
                        hasTaxes("USD", 2.00 + 0.11 + 3.00), hasFees("USD", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.USD);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende21()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7561091049"), hasWkn("899744"), hasTicker(null), //
                        hasName("Realty Income Corp."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-15T00:00"), hasShares(14.00), //
                        hasSource("Dividende21.txt"), //
                        hasNote("Vorgangs-Nr.: XXX"), //
                        hasAmount("USD", 2.66), hasGrossValue("USD", 3.58), //
                        hasTaxes("USD", 0.36 + 0.02 + 0.54), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende21WithSecurityInEUR()
    {
        Security security = new Security("Realty Income Corp.", CurrencyUnit.EUR);
        security.setIsin("US7561091049");
        security.setWkn("899744");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-15T00:00"), hasShares(14.00), //
                        hasSource("Dividende21.txt"), //
                        hasNote("Vorgangs-Nr.: XXX"), //
                        hasAmount("USD", 2.66), hasGrossValue("USD", 3.58), //
                        hasForexGrossValue("EUR", 3.28), //
                        hasTaxes("USD", 0.36 + 0.02 + 0.54), hasFees("USD", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.USD);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende22()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende22.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US698299AW45"), hasWkn("A0GMJZ"), hasTicker(null), //
                        hasName("Panama, Republik DL-Bonds 2006(34-36)"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-26T00:00"), hasShares(10.00), //
                        hasSource("Dividende22.txt"), //
                        hasNote("Vorgangs-Nr.: 25694016"), //
                        hasAmount("USD", 33.50), hasGrossValue("USD", 33.50), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende22WithSecurityInEUR()
    {
        Security security = new Security("Panama, Republik DL-Bonds 2006(34-36)", CurrencyUnit.EUR);
        security.setIsin("US698299AW45");
        security.setWkn("A0GMJZ");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende22.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-26T00:00"), hasShares(10.00), //
                        hasSource("Dividende22.txt"), //
                        hasNote("Vorgangs-Nr.: 25694016"), //
                        hasAmount("USD", 33.50), hasGrossValue("USD", 33.50), //
                        hasForexGrossValue("EUR", 30.81), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.USD);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende23()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US92936U1097"), hasWkn("A1J5SB"), hasTicker(null), //
                        hasName("W.P. Carey Inc."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-10-16T00:00"), hasShares(28.00), //
                        hasSource("Dividende23.txt"), //
                        hasNote("Vorgangs-Nr.: 22556339"), //
                        hasAmount("EUR", 4.88), hasGrossValue("EUR", 4.88), //
                        hasForexGrossValue("USD", 5.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende23WithSecurityInEUR()
    {
        Security security = new Security("W.P. Carey Inc.", CurrencyUnit.EUR);
        security.setIsin("US92936U1097");
        security.setWkn("A1J5SB");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-10-16T00:00"), hasShares(28.00), //
                        hasSource("Dividende23.txt"), //
                        hasNote("Vorgangs-Nr.: 22556339"), //
                        hasAmount("EUR", 4.88), hasGrossValue("EUR", 4.88), //
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
    public void testDividendeStorno01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005785604"), hasWkn("578560"), hasTicker(null), //
                        hasName("Fresenius SE & Co. KGaA"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2022-06-13"), hasShares(10.672), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote("Vorgangs-Nr.: XXXXX"), //
                                        hasAmount("EUR", 7.04), hasGrossValue("EUR", 7.04), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividendeStorno02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US85571B1052"));
        assertThat(security.getWkn(), is("A0N9JF"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Starwood Property Trust Inc."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check cancellation (Storno) transaction
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((AccountTransaction) cancellation.getSubject()).getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorOrderCancellationUnsupported));

        assertThat(((Transaction) cancellation.getSubject()).getDateTime(), is(LocalDateTime.parse("2021-10-18T00:00")));
        assertThat(((Transaction) cancellation.getSubject()).getShares(), is(Values.Share.factorize(250)));
        assertThat(((Transaction) cancellation.getSubject()).getSource(), is("DividendeStorno02.txt"));
        assertThat(((Transaction) cancellation.getSubject()).getNote(), is("Vorgangs-Nr.: 6662228"));

        assertThat(((Transaction) cancellation.getSubject()).getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(76.34))));
        assertThat(((Transaction) cancellation.getSubject()).getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(103.45))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.11 + 0.92 + 0.56 + 15.52))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = ((Transaction) cancellation.getSubject()).getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(120.00))));
    }

    @Test
    public void testDividendeStorno03()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7561091049"), hasWkn("899744"), hasTicker(null), //
                        hasName("Realty Income Corp."), //
                        hasCurrencyCode("USD"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2022-10-14T00:00"), hasShares(5.683), //
                                        hasSource("DividendeStorno03.txt"), //
                                        hasNote("Vorgangs-Nr.: 2546"), //
                                        hasAmount("EUR", 1.14), hasGrossValue("EUR", 1.34), hasForexGrossValue("USD", 1.32), //
                                        hasTaxes("EUR", 0.20), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividendeStorno03WithSecurityInEUR()
    {
        Security security = new Security("Realty Income Corp.", CurrencyUnit.EUR);
        security.setIsin("US7561091049");
        security.setWkn("899744");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2022-10-14T00:00"), hasShares(5.683), //
                                        hasSource("DividendeStorno03.txt"), //
                                        hasNote("Vorgangs-Nr.: 2546"), //
                                        hasAmount("EUR", 1.14), hasGrossValue("EUR", 1.34), //
                                        hasTaxes("EUR", 0.20), hasFees("EUR", 0.00), //
                                        check(tx -> {
                                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                                            Account account = new Account();
                                            account.setCurrencyCode(CurrencyUnit.EUR);
                                            Status s = c.process((AccountTransaction) tx, account);
                                            assertThat(s, is(Status.OK_STATUS));
                                        })))));
    }

    @Test
    public void testDividendeStorno04()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US29670E1073"), hasWkn("A2JN57"), hasTicker(null), //
                        hasName("Essential Properties Real.Tr."), //
                        hasCurrencyCode("USD"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2023-01-13T00:00"), hasShares(39.00), //
                                        hasSource("DividendeStorno04.txt"), //
                                        hasNote("Vorgangs-Nr.: 19658986"), //
                                        hasAmount("EUR", 1.45), hasGrossValue("EUR", 2.00), //
                                        hasForexGrossValue("USD", 2.17), //
                                        hasTaxes("EUR", 0.49 + 0.04 + 0.02), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividendeStorno04WithSecurityInEUR()
    {
        Security security = new Security("Essential Properties Real.Tr.",
                        CurrencyUnit.EUR);
        security.setIsin("US29670E1073");
        security.setWkn("A2JN57");

        Client client = new Client();
        client.addSecurity(security);

        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2023-01-13T00:00"), hasShares(39.00), //
                                        hasSource("DividendeStorno04.txt"), //
                                        hasNote("Vorgangs-Nr.: 19658986"), //
                                        hasAmount("EUR", 1.45), hasGrossValue("EUR", 2.00), //
                                        hasTaxes("EUR", 0.49 + 0.04 + 0.02), hasFees("EUR", 0.00), //
                                        check(tx -> {
                                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                                            Account account = new Account();
                                            account.setCurrencyCode(CurrencyUnit.EUR);
                                            Status s = c.process((AccountTransaction) tx, account);
                                            assertThat(s, is(Status.OK_STATUS));
                                        })))));
    }

    @Test
    public void testDividendeStorno05()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000LEG1110"), hasWkn("LEG111"), hasTicker(null), //
                        hasName("LEG Immobilien SE"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2024-06-26T00:00"), hasShares(4.003), //
                                        hasSource("DividendeStorno05.txt"), //
                                        hasNote("Vorgangs-Nr.: 99999999"), //
                                        hasAmount("EUR", 9.81), hasGrossValue("EUR", 9.81), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testSteuerausgleichsrechnung01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerausgleichsrechnung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-22T00:00")));
        assertThat(transaction.getSource(), is("Steuerausgleichsrechnung01.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 12345"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.01))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.01))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSteuerausgleichsrechnung02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerausgleichsrechnung02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-26T00:00")));
        assertThat(transaction.getSource(), is("Steuerausgleichsrechnung02.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 11111"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(29.06))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(29.06))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSteuerausgleichsrechnung03()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerausgleichsrechnung03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-22T00:00")));
        assertThat(transaction.getSource(), is("Steuerausgleichsrechnung03.txt"));
        assertThat(transaction.getNote(), is("Vorgangs-Nr.: 1234567"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.69))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.69))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testSteuerausgleichsrechnung04()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerausgleichsrechnung04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-01-30T00:00"), hasShares(0), //
                        hasSource("Steuerausgleichsrechnung04.txt"), //
                        hasNote("Vorgangs-Nr.: 6859843 | Vorgangs-Nr.: 48132680 Fondsfusion | LU1861134382"), //
                        hasAmount("EUR", 59.39), hasGrossValue("EUR", 59.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerausgleichsrechnung05()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerausgleichsrechnung05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-01-30T00:00"), hasShares(0), //
                        hasSource("Steuerausgleichsrechnung05.txt"), //
                        hasNote("Vorgangs-Nr.: 0971332 | LU1861134382"), //
                        hasAmount("EUR", 59.38), hasGrossValue("EUR", 59.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testPeriodenauszug01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Periodenauszug01.txt"), errors);

        assertThat(results.size(), is(1));
        assertThat(errors, empty());
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-04-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10000.00))));
        assertThat(transaction.getSource(), is("Periodenauszug01.txt"));
        assertThat(transaction.getNote(), is("Lastschrift aktiv"));
    }

    @Test
    public void testPeriodenauszug02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Periodenauszug02.txt"), errors);

        assertThat(results.size(), is(3));
        assertThat(errors, empty());
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-07-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.97))));
        assertThat(transaction.getSource(), is("Periodenauszug02.txt"));
        assertThat(transaction.getNote(), is("Lastschrift aktiv"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-08-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(transaction.getSource(), is("Periodenauszug02.txt"));
        assertThat(transaction.getNote(), is("SEPA-Ueberweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-07-10T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.56))));
        assertThat(transaction.getSource(), is("Periodenauszug02.txt"));
        assertThat(transaction.getNote(), is("Transaktionskostenpauschale o. MwSt."));
    }

    @Test
    public void testPeriodenauszug03()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Periodenauszug03.txt"), errors);

        assertThat(results.size(), is(1));
        assertThat(errors, empty());
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-05-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("Periodenauszug03.txt"));
        assertThat(transaction.getNote(), is("Lastschrift aktiv"));
    }

    @Test
    public void testPeriodenauszug04()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Periodenauszug04.txt"), errors);

        assertThat(results.size(), is(3));
        assertThat(errors, empty());
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Periodenauszug04.txt"));
        assertThat(transaction.getNote(), is("Credit SEPA"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
        assertThat(transaction.getSource(), is("Periodenauszug04.txt"));
        assertThat(transaction.getNote(), is("Direct Debit"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-28T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Periodenauszug04.txt"));
        assertThat(transaction.getNote(), is("Credit SEPA"));
    }

    @Test
    public void testPeriodenauszug05()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Periodenauszug05.txt"), errors);

        assertThat(results.size(), is(1));
        assertThat(errors, empty());
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.99))));
        assertThat(transaction.getSource(), is("Periodenauszug05.txt"));
        assertThat(transaction.getNote(), is("Ordergebühr | Ref.-Nr.: SCAL4Pm2ZTfDF4c"));
    }

    @Test
    public void testPeriodenauszug06()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Periodenauszug06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-12-21"), hasAmount("EUR", 100.00), //
                        hasSource("Periodenauszug06.txt"), hasNote("Direct Debit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-12-21"), hasAmount("EUR", 1.00), //
                        hasSource("Periodenauszug06.txt"), hasNote("Direct Debit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-12-21"), hasAmount("EUR", 500.00), //
                        hasSource("Periodenauszug06.txt"), hasNote("Credit SEPA"))));
    }

    @Test
    public void testPeriodenauszug07()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Periodenauszug07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-03"), hasAmount("EUR", 200.00), //
                        hasSource("Periodenauszug07.txt"), hasNote("Direct Debit"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-06"), hasAmount("EUR", 10000.00), //
                        hasSource("Periodenauszug07.txt"), hasNote("Credit SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-20"), hasAmount("EUR", 100.00), //
                        hasSource("Periodenauszug07.txt"), hasNote("Direct Debit"))));
    }

    @Test
    public void testPeriodenauszug08()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Periodenauszug08.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-31"), hasAmount("EUR", 356.29), //
                        hasSource("Periodenauszug08.txt"), hasNote("Lastschrift aktiv"))));
    }

    @Test
    public void testPeriodenauszug09()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Periodenauszug09.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-02"), hasAmount("EUR", 25.00), //
                        hasSource("Periodenauszug09.txt"), hasNote("Lastschrift aktiv"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-02"), hasAmount("EUR", 100.00), //
                        hasSource("Periodenauszug09.txt"), hasNote("Lastschrift aktiv"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-02"), hasAmount("EUR", 25.00), //
                        hasSource("Periodenauszug09.txt"), hasNote("Lastschrift aktiv"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-02"), hasAmount("EUR", 25.00), //
                        hasSource("Periodenauszug09.txt"), hasNote("Lastschrift aktiv"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-02"), hasAmount("EUR", 35.00), //
                        hasSource("Periodenauszug09.txt"), hasNote("Lastschrift aktiv"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-02"), hasAmount("EUR", 40.00), //
                        hasSource("Periodenauszug09.txt"), hasNote("Lastschrift aktiv"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-02"), hasAmount("EUR", 50.00), //
                        hasSource("Periodenauszug09.txt"), hasNote("Lastschrift aktiv"))));
    }

    @Test
    public void testPeriodenauszug10()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Periodenauszug10.txt"),
                        errors);

        // Check if the results list is not empty
        assertTrue(results.isEmpty());

        // Check if at least one error is present
        assertTrue(!errors.isEmpty());

        // Extract the first error from the list
        Exception firstError = errors.get(0);

        // Check if the first error is an UnsupportedOperationException
        assertTrue(firstError instanceof UnsupportedOperationException);

        // Check the error message of the first error
        String expectedErrorMessage = MessageFormat.format(Messages.PDFdbMsgCannotDetermineFileType,
                        "Baader Bank AG / Scalable Capital Vermögensverwaltung GmbH / Traders Place GmbH & Co. KGaA", "Periodenauszug10.txt");
        assertEquals(expectedErrorMessage, firstError.getMessage());
    }

    @Test
    public void testPeriodenauszug11()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Periodenauszug11.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-05"), hasAmount("EUR", 28240.00), //
                        hasSource("Periodenauszug11.txt"), hasNote("Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-07-15"), hasAmount("EUR", 0.99), //
                        hasSource("Periodenauszug11.txt"), hasNote("Ordergebühr | Ref.-Nr.: SCALANDnkDU5Lx8"))));
    }

    @Test
    public void testPeriodenauszug12()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Periodenauszug12.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-27"), hasAmount("EUR", 164.78), //
                        hasSource("Periodenauszug12.txt"), hasNote("Gutschrift"))));
    }

    @Test
    public void testTageskontoauszug01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tageskontoauszug01.txt"), errors);

        assertThat(results.size(), is(1));
        assertThat(errors, empty());
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-11T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.00))));
        assertThat(transaction.getSource(), is("Tageskontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutschrift"));
    }

    @Test
    public void testRechnung01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-08-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.48))));
        assertThat(transaction.getSource(), is("Rechnung01.txt"));
        assertThat(transaction.getNote(), is("Rechnungsnr.: 2017071234567 | Abrechnungszeitraum 01.07.2017 - 31.07.2017"));
    }

    @Test
    public void testRechnungsabschluss01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rechnungsabschluss01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-10-31T00:00"), //
                        hasSource("Rechnungsabschluss01.txt"), //
                        hasNote("Vorgangs-Nr.: 12345678 | 30.09.2023 bis 31.10.2023"), //
                        hasAmount("EUR", 1.46), hasGrossValue("EUR", 2.00), //
                        hasTaxes("EUR", 0.49 + 0.03 + 0.02), hasFees("EUR", 0.00))));
    }

    @Test
    public void testEinbuchung01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Einbuchung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("DE000A3H3MF2"));
        assertThat(security1.getWkn(), is("A3H3MF"));
        assertNull(security1.getTickerSymbol());
        assertThat(security1.getName(), is("Enapter AG Inhaber-Bezugsrechte"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("DE000A3H3MG0"));
        assertThat(security2.getWkn(), is("A3H3MG"));
        assertNull(security2.getTickerSymbol());
        assertThat(security2.getName(), is("Enapter AG junge Inhaber-Aktien o.N."));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check delivery outbound (Auslieferung) transaction
        PortfolioTransaction entry = (PortfolioTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));

        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2021-03-09T00:00")));
        assertThat(entry.getShares(), is(Values.Share.factorize(96)));
        assertThat(entry.getSource(), is("Einbuchung01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check delivery inbound (Einlieferung) transaction
        entry = (PortfolioTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2021-03-09T00:00")));
        assertThat(entry.getShares(), is(Values.Share.factorize(6)));
        assertThat(entry.getSource(), is("Einbuchung01.txt"));
        assertThat(entry.getNote(), is("Bezugsverhältnis: 16 : 1"));

        assertThat(entry.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(132.00))));
        assertThat(entry.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(132.00))));
        assertThat(entry.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testFusion01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fusion01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1861134382"), hasWkn("A2JSDA"), hasTicker(null), //
                        hasName("AIS-AM.WORLD SRI PAB Act.Nom. UCITS ETF DR (C)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                                        hasDate("2024-01-18T00:00"), hasShares(277.691), //
                                        hasSource("Fusion01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFusion02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fusion02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0635178014"), hasWkn("ETF127"), hasTicker(null), //
                        hasName("Lyxor MSCI Emerg.Mkts U.ETF Inh.-An. I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                                        hasDate("2023-03-24T00:00"), hasShares(2.00), //
                                        hasSource("Fusion02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDepoteinlieferung01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depoteinlieferung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JL35"), hasWkn("A1103D"), hasTicker(null), //
                        hasName("Xtr.(IE) - MSCI World Quality Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                                        hasDate("2023-01-25T00:00"), hasShares(1.00), //
                                        hasSource("Depoteinlieferung01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDepoteinlieferung02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depoteinlieferung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKM4GZ66"), hasWkn("A111X9"), hasTicker(null), //
                        hasName("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                                        hasDate("2022-07-15T00:00"), hasShares(1.00), //
                                        hasSource("Depoteinlieferung02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDepoteinlieferung03()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depoteinlieferung03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BJ0KDQ92"), hasWkn("A1XB5U"), hasTicker(null), //
                        hasName("Xtr.(IE) - MSCI World Registered Shares 1C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                                        hasDate("2022-03-30T00:00"), hasShares(3.00), //
                                        hasSource("Depoteinlieferung03.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testSplit01()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

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
                        hasIsin("US0465131078"), hasWkn("A1166A"), hasTicker(null), //
                        hasName("Atara Biotherapeutics Inc. Registered Shares DL -,0001"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorSplitTransactionsNotSupported, //
                        outboundDelivery( //
                                        hasDate("2024-06-20T00:00"), hasShares(700.00), //
                                        hasSource("Split01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testSplit02()
    {
        BaaderBankPDFExtractor extractor = new BaaderBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Split02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2360697374"), hasWkn("A3CT9J"), hasTicker(null), //
                        hasName("FREYR Battery Actions Nominatives DL 1"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorSplitTransactionsNotSupported, //
                        outboundDelivery( //
                                        hasDate("2024-01-02T00:00"), hasShares(63.00), //
                                        hasSource("Split02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }
}
