package name.abuchen.portfolio.datatransfer.pdf.dkb;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
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
import name.abuchen.portfolio.datatransfer.pdf.DkbPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DkbPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("DE000A1HLTD2"));
        assertThat(security.getWkn(), is("A1HLTD"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("8,75 % METALCORP GROUP B.V. EO-ANLEIHE 2013(18)"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-11-25T11:02:54")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 495752/48.00 | Limit 97,50 %"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(2030.66))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(2023.16))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(7.50))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("BMG7945E1057"));
        assertThat(security.getWkn(), is("A0ERZ0"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("SEADRILL LTD. REGISTERED SHARES DL 2,-"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-25T09:33:06")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1000)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Limit 1,75 EUR"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1760.91))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(1750.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(10.00 + 0.71 + 0.20))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("COMSTAGE-MSCI WORLD TRN U.ETF INHABER-ANTEILE I O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(29.2893)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1410.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(1400.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(10.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("COMSTAGE-MSCI WORLD TRN U.ETF INHABER-ANTEILE I O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-10-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.521)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(130.41))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(128.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.50 + 0.71 + 0.20))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("COMSTAGE-MSCI WORLD TRN U.ETF INHABER-ANTEILE I O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-01-07T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25.6)));
        assertThat(entry.getSource(), is("Kauf05.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1201.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(1200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.50))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("COMSTAGE-MSCI WORLD TRN U.ETF NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-07T11:11:56")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
        assertThat(entry.getSource(), is("Kauf06.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1144.20))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(1142.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(1.00 + 0.60))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("FR0000121014"));
        assertThat(security.getWkn(), is("853292"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("LVMH MOET HENN. L. VUITTON SE ACTIONS PORT. (C.R.) EO 0,3"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-09-01T08:00:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));
        assertThat(entry.getSource(), is("Kauf07.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1918.31))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(1902.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(5.71))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(10.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("CH0126639464"));
        assertThat(security.getWkn(), is("A1JJES"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("CALIDA HOLDING AG NAM.-AKT. SF 0,10"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-01-09T17:14:41")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Kauf08.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 123456/78.90 | Limit 50,00 CHF"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1014.68))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(984.79))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(20.00 + 9.89))));

        var grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CHF", Values.Amount.factorize(971.00))));
    }

    @Test
    public void testWertpapierKauf08WithSecurityInEUR()
    {
        var security = new Security("CALIDA HOLDING AG NAM.-AKT. SF 0,10", "EUR");
        security.setIsin("CH0126639464");
        security.setWkn("A1JJES");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

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
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-01-09T17:14:41")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Kauf08.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 123456/78.90 | Limit 50,00 CHF"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1014.68))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(984.79))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(20.00 + 9.89))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("US88080T1043"), hasWkn("A3C9C7"), hasTicker(null), //
                        hasName("TERAWULF INC. REGISTERED SHARES DL -,10"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-01-31T19:46:30"), hasShares(2300), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Limit 1,80 USD"), //
                        hasAmount("EUR", 3884.81), hasGrossValue("EUR", 3848.30), //
                        hasForexGrossValue("USD", 4140.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00 + 6.51 + 20.00))));
    }

    @Test
    public void testWertpapierKauf09WithSecurityInEUR()
    {
        var security = new Security("TERAWULF INC. REGISTERED SHARES DL -,10", "EUR");
        security.setIsin("US88080T1043");
        security.setWkn("A3C9C7");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

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
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-01-31T19:46:30"), hasShares(2300), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Limit 1,80 USD"), //
                        hasAmount("EUR", 3884.81), hasGrossValue("EUR", 3848.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00 + 6.51 + 20.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("US75629F1093"), hasWkn("A1XFAV"), hasTicker(null), //
                        hasName("SOCIETAL CDMO INC. REGISTERED SHARES DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-02-27T19:42:48"), hasShares(2000), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Auftragsnummer 123456/31.01 | Limit 0,54 USD"), //
                        hasAmount("EUR", 1037.14), hasGrossValue("EUR", 1000.65), //
                        hasForexGrossValue("USD", 1079.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00 + 6.49 + 20.00))));
    }

    @Test
    public void testWertpapierKauf10WithSecurityInEUR()
    {
        var security = new Security("SOCIETAL CDMO INC. REGISTERED SHARES DL -,01", "EUR");
        security.setIsin("US75629F1093");
        security.setWkn("A1XFAV");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

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
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-02-27T19:42:48"), hasShares(2000), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Auftragsnummer 123456/31.01 | Limit 0,54 USD"), //
                        hasAmount("EUR", 1037.14), hasGrossValue("EUR", 1000.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00 + 6.49 + 20.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

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
        assertThat(security.getIsin(), is("AT0000A0U9J2"));
        assertThat(security.getWkn(), is("A1MLSS"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("8,5 % SCHOLZ HOLDING INH.-SCHV. V.2012(2017)"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-10-27T09:05:33")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(60)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 495752/36.00 | Limit 85,00 %"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(4937.19))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(5428.36))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(420.24 + 23.11 + 37.82))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(10.00))));

        // check tax refund transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-10-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(60)));
        assertThat(transaction.getSource(), is("Verkauf01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(56.57))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(56.57))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005140008"));
        assertThat(security.getWkn(), is("514000"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("DEUTSCHE BANK AG NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-02-10T10:58:02")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 590966/56.00 | Limit 15,05 EUR"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(3000.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(3010.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(10.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A2YN900"));
        assertThat(security.getWkn(), is("A2YN90"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("TEAMVIEWER AG INHABER-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-06T14:32:46")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getSource(), is("Verkauf03.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 123456/12.34"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(4123.12))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(4245.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(109.37 + 6.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(7.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

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
        assertThat(security.getIsin(), is("US00165C1045"));
        assertThat(security.getWkn(), is("A1W90H"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("AMC ENTERTAINMENT HOLDINGS INC REG. SHARES CLASS A DL -,01"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-22T20:56:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Verkauf04.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 123456/78.00 | Limit 5,44 EUR"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(99.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(109.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(10.00))));

        // check tax refund transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20)));
        assertThat(transaction.getSource(), is("Verkauf04.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(16.08))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(16.08))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("DE000LED02V0"));
        assertThat(security.getWkn(), is("LED02V"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("OSRAM LICHT AG Z.VERKAUF EING.NAMENS-AKTIEN"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-09T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        assertThat(entry.getSource(), is("Verkauf05.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(164.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(164.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

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
        assertThat(security.getIsin(), is("CA3012831077"));
        assertThat(security.getWkn(), is("A1C30Q"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("EXCHANGE INCOME CORP. REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-07-06T14:49:39")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(75)));
        assertThat(entry.getSource(), is("Verkauf06.txt"));
        assertThat(entry.getNote(), is("Limit 27,80 EUR"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(2051.02))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(2085.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(19.63 + 1.07))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(10.00 + 0.06 + 1.55 + 1.67))));
    }

    @Test
    public void testWertpapierVerkauf07()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

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
        assertThat(security.getIsin(), is("FR0010524777"));
        assertThat(security.getWkn(), is("LYX0CB"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("LYXOR NEW ENERGY UCITS ETF ACTIONS AU PORT.DIST O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-10-07T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.982)));
        assertThat(entry.getSource(), is("Verkauf07.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 000000/00.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(24.69))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(24.79))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.10))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf08()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf08.txt"), errors);

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
        assertThat(security.getIsin(), is("XS0149161217"));
        assertThat(security.getWkn(), is("858865"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("2,309 % RBS CAPITAL TRUST A EO-FLR TR.PREF.SEC.02(12/UND.)"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2014-07-31T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));
        assertThat(entry.getSource(), is("Verkauf08.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 9892578200 | Rückzahlungskurs 100 %"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(2974.39))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(3000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(24.28 + 1.33))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf09()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A1RE7V0"));
        assertThat(security.getWkn(), is("A1RE7V"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("6,875 % MS DEUTSCHLAND GMBH INH.-SCHV. V.2012(2017)"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-13T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Verkauf09.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 9796635900 | Rückzahlungskurs 100 %"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1908.39))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(2000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(80.01 + 4.40 + 7.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf10()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A1RE7V0"));
        assertThat(security.getWkn(), is("A1RE7V"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("6,875 % MS DEUTSCHLAND GMBH INH.-SCHV. V.2012(2017)"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-13T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Verkauf10.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 9796635950 | Rückzahlungskurs 100 %"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1908.39))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(2000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(80.01 + 4.40 + 7.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf11()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf11.txt"), errors);

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
        assertThat(security.getIsin(), is("DE000A1RE7V0"));
        assertThat(security.getWkn(), is("A1RE7V"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("6,875 % MS DEUTSCHLAND GMBH INH.-SCHV. V.2012(2017)"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-01-13T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Verkauf11.txt"));
        assertThat(entry.getNote(), is("Rückzahlungskurs 100 %"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(2000.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(2000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf12()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf12.txt"), errors);

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
                        hasIsin("DE000VC5YHF3"), hasWkn("VC5YHF"), hasTicker(null), //
                        hasName("VONTOBEL FINANCIAL PRODUCTS DIZ 26.09.25 TOTAL 48"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-09-26T00:00"), hasShares(50.00), //
                        hasSource("Verkauf12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2390.77), hasGrossValue("EUR", 2400.00), //
                        hasTaxes("EUR", 8.75 + 0.48), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufStorno01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "VerkaufStorno01.txt"), errors);

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
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BKX55T58"));
        assertThat(security.getWkn(), is("A12CX1"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("VANG.FTSE DEVELOP.WORLD U.ETF REGISTERED SHARES USD DIS.ON"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check cancellation (Storno) transaction
        var cancellation = (BuySellEntryItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(BuySellEntryItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((BuySellEntry) cancellation.getSubject()).getPortfolioTransaction().getType(),
                        is(PortfolioTransaction.Type.BUY));
        assertThat(((BuySellEntry) cancellation.getSubject()).getAccountTransaction().getType(),
                        is(AccountTransaction.Type.BUY));
        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorOrderCancellationUnsupported));

        assertThat(((BuySellEntry) cancellation.getSubject()).getPortfolioTransaction().getDateTime(),
                        is(LocalDateTime.parse("2021-01-05T00:00")));
        assertThat(((BuySellEntry) cancellation.getSubject()).getPortfolioTransaction().getShares(),
                        is(Values.Share.factorize(16.3986)));
        assertThat(((BuySellEntry) cancellation.getSubject()).getSource(), is("VerkaufStorno01.txt"));
        assertNull(((BuySellEntry) cancellation.getSubject()).getNote());

        assertThat(((BuySellEntry) cancellation.getSubject()).getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(1051.50))));
        assertThat(((BuySellEntry) cancellation.getSubject()).getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(1051.50))));
        assertThat(((BuySellEntry) cancellation.getSubject()).getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(((BuySellEntry) cancellation.getSubject()).getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("DE000A1R1AN5"));
        assertThat(security.getWkn(), is("A1R1AN"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("PCC SE INH.-TEILSCHULDV. V.13(13/17)"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-01-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 86525618110"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(144.52))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(181.25))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(32.09 + 1.76 + 2.88))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("DE0007100000"));
        assertThat(security.getWkn(), is("710000"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("DAIMLER AG NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-04-07T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(30)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 59717175720"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(97.50))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(97.50))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("IE00B3XNN521"));
        assertThat(security.getWkn(), is("A1JJAG"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("DIM.FDS-GLOBAL SMALL COMPANIES REGISTERED SHARES EUR DIS.O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-12-07T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(216)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 84033925310"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(32.93))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(45.61))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(11.18 + 0.61 + 0.89))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("CH0010570767"));
        assertThat(security.getWkn(), is("870503"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-05-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 63136911234"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(27.72))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(42.65))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(14.93))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CHF", Values.Amount.factorize(51.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10", "EUR");
        security.setIsin("CH0010570767");
        security.setWkn("870503");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

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

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-05-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 63136911234"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(27.72))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(42.65))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(14.93))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("IE00B4L5Y983"));
        assertThat(security.getWkn(), is("A0RPWH"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShares Core MSCI World UCITS ETF USD REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-06-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 12345678901"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(3.02))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(4.10))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(1.03 + 0.05))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("IE00B3VVMM84"));
        assertThat(security.getWkn(), is("A1JX51"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("VANGUARD FTSE EM.MARKETS U.ETF REGISTERED SHARES USD DIS.ON"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-01-11T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(16.3517)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 11111"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(2.45))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(2.99))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.52 + 0.02))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(3.46))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        var security = new Security("VANGUARD FTSE EM.MARKETS U.ETF REGISTERED SHARES USD DIS.ON", "EUR");
        security.setIsin("IE00B3VVMM84");
        security.setWkn("A1JX51");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

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

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-01-11T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(16.3517)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 11111"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(2.45))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(2.99))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.52 + 0.02))));
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
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("DE000A1R1AN5"));
        assertThat(security.getWkn(), is("A1R1AN"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("PCC SE INH.-TEILSCHULDV. V.13(13/17)"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-01-02T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende07.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 86508012450"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(173.02))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(181.25))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(7.81 + 0.42))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("DE0005552004"));
        assertThat(security.getWkn(), is("555200"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("DEUTSCHE POST AG NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-20T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(92)));
        assertThat(transaction.getSource(), is("Dividende08.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 63736123456"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(105.80))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(105.80))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("DE000A0LGQL5"));
        assertThat(security.getWkn(), is("A0LGQL"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("IS.II-DEV.MARK.PR.YLD. UC. ETF BEARER SHARES (DT. ZERT.) O.N."));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-02-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(53)));
        assertThat(transaction.getSource(), is("Dividende09.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 8127381273"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(7.79))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(8.53))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.85 / 1.1426))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(8.53 * 1.1426))));
    }

    @Test
    public void testDividende09WithSecurityInEUR()
    {
        var security = new Security("IS.II-DEV.MARK.PR.YLD. UC. ETF BEARER SHARES (DT. ZERT.) O.N.", "EUR");
        security.setIsin("DE000A0LGQL5");
        security.setWkn("A0LGQL");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

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

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-02-23T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(53)));
        assertThat(transaction.getSource(), is("Dividende09.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 8127381273"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(7.79))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(8.53))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.85 / 1.1426))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("IE00B6YX5D40"));
        assertThat(security.getWkn(), is("A1JKS0"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("SPDR S&P US DIVID.ARISTOCR.ETF REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-10-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10.6841)));
        assertThat(transaction.getSource(), is("Dividende10.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 123456789"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(2.09))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(2.48))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.46 / 1.1780))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(2.48 * 1.1780))));
    }

    @Test
    public void testDividende10WithSecurityInEUR()
    {
        var security = new Security("SPDR S&P US DIVID.ARISTOCR.ETF REGISTERED SHARES O.N.", "EUR");
        security.setIsin("IE00B6YX5D40");
        security.setWkn("A1JKS0");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

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

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-10-04T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(10.6841)));
        assertThat(transaction.getSource(), is("Dividende10.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 123456789"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(2.09))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(2.48))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.46 / 1.1780))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende11()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("US0378331005"));
        assertThat(security.getWkn(), is("865985"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("APPLE INC. REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-02-20T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(66)));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 123456789 | Quartalsdividende"));
        assertThat(transaction.getSource(), is("Dividende11.txt"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(29.95))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(35.24))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(5.29))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(37.62))));
    }

    @Test
    public void testDividende11WithSecurityInEUR()
    {
        var security = new Security("APPLE INC. REGISTERED SHARES O.N.", "EUR");
        security.setIsin("US0378331005");
        security.setWkn("865985");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

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

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-02-20T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(66)));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 123456789 | Quartalsdividende"));
        assertThat(transaction.getSource(), is("Dividende11.txt"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(29.95))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(35.24))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(5.29))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende12()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

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
        assertThat(security.getIsin(), is("JP3414750004"));
        assertThat(security.getWkn(), is("471496"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("SEIKO EPSON CORP. REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is("JPY"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(715)));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 111111111 | Schlussdividende"));
        assertThat(transaction.getSource(), is("Dividende12.txt"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(133.58))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(180.17))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(27.03 + 18.01 + 0.99 + 0.56))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("JPY", Values.Amount.factorize(22165.00))));
    }

    @Test
    public void testDividende12WithSecurityInEUR()
    {
        var security = new Security("SEIKO EPSON CORP. REGISTERED SHARES O.N.", "EUR");
        security.setIsin("JP3414750004");
        security.setWkn("471496");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

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

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-01T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(715)));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 111111111 | Schlussdividende"));
        assertThat(transaction.getSource(), is("Dividende12.txt"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(133.58))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(180.17))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(27.03 + 18.01 + 0.99 + 0.56))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende13()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("GB00B02J6398"));
        assertThat(security.getWkn(), is("A0DJ58"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ADMIRAL GROUP PLC REGISTERED SHARES LS -,001"));
        assertThat(security.getCurrencyCode(), is("GBP"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-10-13T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(450)));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 12345678901 | Zwischendividende"));
        assertThat(transaction.getSource(), is("Dividende13.txt"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(227.63))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(309.17))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(77.29 + 4.25))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("GBP", Values.Amount.factorize(229.50))));
    }

    @Test
    public void testDividende13WithSecurityInEUR()
    {
        var security = new Security("ADMIRAL GROUP PLC REGISTERED SHARES LS -,001", "EUR");
        security.setIsin("GB00B02J6398");
        security.setWkn("A0DJ58");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

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

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-10-13T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(450)));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 12345678901 | Zwischendividende"));
        assertThat(transaction.getSource(), is("Dividende13.txt"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(227.63))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(309.17))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(77.29 + 4.25))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende14()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("DE000A1R1AN5"));
        assertThat(security.getWkn(), is("A1R1AN"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("PCC SE INH.-TEILSCHULDV. V.13(13/17)"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-05T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(935)));
        assertThat(transaction.getSource(), is("Dividende14.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 12345678901"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(13.24))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(18.36))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(2.24 + 2.24 + 0.12 + 0.12 + 0.20 + 0.20))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(22.16))));
    }

    @Test
    public void testDividende14WithSecurityInEUR()
    {
        var security = new Security("PCC SE INH.-TEILSCHULDV. V.13(13/17)", "EUR");
        security.setIsin("DE000A1R1AN5");
        security.setWkn("A1R1AN");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

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

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-05T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(935)));
        assertThat(transaction.getSource(), is("Dividende14.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 12345678901"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(13.24))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(18.36))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(2.24 + 2.24 + 0.12 + 0.12 + 0.20 + 0.20))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende15()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("CH0010570767"));
        assertThat(security.getWkn(), is("870503"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10"));
        assertThat(security.getCurrencyCode(), is("CHF"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-05-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getSource(), is("Dividende15.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 63136911234 | Kapitalrückzahlung"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(35.12))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(35.12))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CHF", Values.Amount.factorize(42.00))));
    }

    @Test
    public void testDividende15WithSecurityInEUR()
    {
        var security = new Security("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10", "EUR");
        security.setIsin("CH0010570767");
        security.setWkn("870503");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

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
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-05-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getSource(), is("Dividende15.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 63136911234 | Kapitalrückzahlung"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(35.12))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(35.12))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende16()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

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
        assertThat(security.getIsin(), is("DE000A0Z2ZZ5"));
        assertThat(security.getWkn(), is("A0Z2ZZ"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("FREENET AG NAMENS-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-05-14T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(200)));
        assertThat(transaction.getSource(), is("Dividende16.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 63310000000"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(290.00))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(290.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende17()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("US56035L1044"), hasWkn("A0X8Y3"), hasTicker(null), //
                        hasName("MAIN STREET CAPITAL CORP. REGISTERED SHARES DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-19T00:00"), hasShares(497.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Abrechnungsnr. 85345940130 | Monatliche Dividende"), //
                        hasAmount("EUR", 141.90), hasGrossValue("EUR", 192.24), //
                        hasForexGrossValue("USD", 211.50), //
                        hasTaxes("EUR", 28.84 + (2 * 9.40) + (2 * 0.51) + (2 * 0.84)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17WithSecurityInEUR()
    {
        var security = new Security("MAIN STREET CAPITAL CORP. REGISTERED SHARES DL -,01", "EUR");
        security.setIsin("US56035L1044");
        security.setWkn("A0X8Y3");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

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
                        hasDate("2023-12-19T00:00"), hasShares(497.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Abrechnungsnr. 85345940130 | Monatliche Dividende"), //
                        hasAmount("EUR", 141.90), hasGrossValue("EUR", 192.24), //
                        hasTaxes("EUR", 28.84 + (2 * 9.40) + (2 * 0.51) + (2 * 0.84)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende18()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("US91282CFM82"), hasWkn("A3K92B"), hasTicker(null), //
                        hasName("UNITED STATES OF AMERICA DL-BONDS 2022(27) S.AD-2027"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-10-01T00:00"), hasShares(180.00), //
                        hasSource("Dividende18.txt"), //
                        hasNote("Abrechnungsnr. 77349248800"), //
                        hasAmount("EUR", 231.64), hasGrossValue("EUR", 314.62), //
                        hasForexGrossValue("USD", 371.25), //
                        hasTaxes("EUR", 78.66 + 4.32), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende18WithSecurityInEUR()
    {
        var security = new Security("UNITED STATES OF AMERICA DL-BONDS 2022(27) S.AD-2027", "EUR");
        security.setIsin("US91282CFM82");
        security.setWkn("A3K92B");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende18.txt"), errors);

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
                        hasDate("2025-10-01T00:00"), hasShares(180.00), //
                        hasSource("Dividende18.txt"), //
                        hasNote("Abrechnungsnr. 77349248800"), //
                        hasAmount("EUR", 231.64), hasGrossValue("EUR", 314.62), //
                        hasTaxes("EUR", 78.66 + 4.32), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierAusgang01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "WertpapierAusgang01.txt"), errors);

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
        assertThat(security.getIsin(), is("DE000US9RGR9"));
        assertThat(security.getWkn(), is("US9RGR"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("24,75 % UBS AG (LONDON BRANCH) EO-ANL. 14(16) RWE"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check transfer_out transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-11-30T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(250)));
        assertThat(entry.getSource(), is("WertpapierAusgang01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 489130/67.00 | Depotkonto-Nr. 100235452280"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
        assertThat(security.getIsin(), is("IE00BK5BQT80"));
        assertThat(security.getWkn(), is("A2PKXG"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("VANGUARD FTSE ALL-WORLD U.ETF REG. SHS USD ACC. ON"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check tax transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-06T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(49.1102)));
        assertThat(transaction.getSource(), is("Vorabpauschale01.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 12345678901"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(0.08))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(0.08))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("DE000A1CUAY0"), hasWkn("A1CUAY"), hasTicker(null), //
                        hasName("WERTGRUND WOHNSELECT D INHABER-ANTEILE"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2024-01-15T00:00"), hasShares(9.6469), //
                                        hasSource("Vorabpauschale02.txt"), //
                                        hasNote("Abrechnungsnr. 123456789"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFondssparplan01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondssparplan01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(6L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0635178014"));
        assertThat(security.getWkn(), is("ETF127"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("COMSTA.-MSCI EM.MKTS.TRN U.ETF"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check 1st buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-07-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.2394)));
        assertThat(entry.getSource(), is("Fondssparplan01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 531781/77.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-08-06T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.1692)));
        assertThat(entry.getSource(), is("Fondssparplan01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 548714/63.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));

        // check 3rd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-09-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.3253)));
        assertThat(entry.getSource(), is("Fondssparplan01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 567153/38.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));

        // check 4th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-10-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.3496)));
        assertThat(entry.getSource(), is("Fondssparplan01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 586519/87.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-11-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.3678)));
        assertThat(entry.getSource(), is("Fondssparplan01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 605113/36.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-12-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.3160)));
        assertThat(entry.getSource(), is("Fondssparplan01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 626208/11.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(90.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testFondssparplan02()
    {
        // Fonds, only 3 decimal places for amount of shares

        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondssparplan02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU1737652583"));
        assertThat(security.getWkn(), is("A2H9Q0"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("AMUNDI IND.SOL.-A.IN.MSCI E.M."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check 1st buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-02-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3.9136)));
        assertThat(entry.getSource(), is("Fondssparplan02.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 256485/46.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(200.49))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.49))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-04-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.8207)));
        assertThat(entry.getSource(), is("Fondssparplan02.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 342983/71.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(200.49))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.49))));

        // check 3rd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-06-22T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.4425)));
        assertThat(entry.getSource(), is("Fondssparplan02.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 414802/35.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(200.49))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(200.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.49))));
    }

    @Test
    public void testGiroKontoauszug01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(10L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-06T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1000)));
        assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-08T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(20)));
        assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-28T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(91)));
        assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Dauerauftrag"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1000)));
        assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Dauerauftrag"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(999)));
        assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Dauerauftrag"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(43.86)));
        assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(50.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-18T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(00.01)));
        assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1234.56)));
        assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Lohn, Gehalt, Rente"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-03T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(105)));
        assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("sonstige Buchung"));
    }

    @Test
    public void testGiroKontoauszug02()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1000)));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1000)));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Dauerauftrag"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1234.56)));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-29T00:00")));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Steuerausgleich"));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.23)));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Steuerausgleich"));
    }

    @Test
    public void testGiroKontoauszug03()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug03.txt"), errors);

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
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(5L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-11T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(31.95)));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-15T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(69.96)));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-18T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1337.96)));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-23T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(163.80)));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-24T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1337.69)));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kreditkartenabrechnung"));
    }

    @Test
    public void testGiroKontoauszug04()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-21T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(3450.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Bareinzahlung am Geldautomat"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-23T00:00")));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Rechnung Bargeldeinzahlung"));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(15.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Rechnung Bargeldeinzahlung"));
    }

    @Test
    public void testGiroKontoauszug05()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(21L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(21));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(21L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.24)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.01.2021 bis 31.03.2021"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-05T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(418.5)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-08T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(150)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Dauerauftrag"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-08T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(200)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Dauerauftrag"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-10T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(82.88)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-11T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(300)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-15T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(20)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-17T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(29.7)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-17T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(42)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-22T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(32.77)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-24T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(28.9)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-24T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(104.21)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Kreditkartenabrechnung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-25T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(28)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(19.54)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(30)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(92.35)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(33.92)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(39.66)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(50)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(23.56)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(52.5)));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));
    }

    @Test
    public void testGiroKontoauszug06()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-08T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(150)));
        assertThat(transaction.getSource(), is("GiroKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(111.11)));
        assertThat(transaction.getSource(), is("GiroKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));
    }

    @Test
    public void testGiroKontoauszug07()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(transaction.getSource(), is("GiroKontoauszug07.txt"));
        assertThat(transaction.getNote(), is("Eingang Echtzeitüberweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1400)));
        assertThat(transaction.getSource(), is("GiroKontoauszug07.txt"));
        assertThat(transaction.getNote(), is("Eingang Echtzeitüberweisung"));
    }

    @Test
    public void testGiroKontoauszug08()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-02T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(300)));
        assertThat(transaction.getSource(), is("GiroKontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-02-27T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(300)));
        assertThat(transaction.getSource(), is("GiroKontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));
    }

    @Test
    public void testGiroKontoauszug09()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.01)));
        assertThat(transaction.getSource(), is("GiroKontoauszug09.txt"));
        assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 03.02.2015 bis 31.03.2015"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-19T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(300)));
        assertThat(transaction.getSource(), is("GiroKontoauszug09.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(34)));
        assertThat(transaction.getSource(), is("GiroKontoauszug09.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-17T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(500)));
        assertThat(transaction.getSource(), is("GiroKontoauszug09.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));
    }

    @Test
    public void testGiroKontoauszug10()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-09-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(0.03))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(0.04))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.01))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug10.txt"));
        assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.07.2015 bis 30.09.2015"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-09-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.18)));
        assertThat(transaction.getSource(), is("GiroKontoauszug10.txt"));
        assertThat(transaction.getNote(), is("Zinsen für Dispositionskredit"));
    }

    @Test
    public void testGiroKontoauszug11()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-12-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(0.16))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(0.22))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.06))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug11.txt"));
        assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.10.2015 bis 31.12.2015"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-12-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.14)));
        assertThat(transaction.getSource(), is("GiroKontoauszug11.txt"));
        assertThat(transaction.getNote(), is("Zinsen für Dispositionskredit"));
    }

    @Test
    public void testGiroKontoauszug12()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug12.txt"), errors);

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
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-06-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(0.14))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.05))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.04.2015 bis 30.06.2015"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-06-11T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(94.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-07-03T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.25)));
        assertThat(transaction.getSource(), is("GiroKontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));
    }

    @Test
    public void testGiroKontoauszug13()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug13.txt"), errors);

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
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-01-27T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug13.txt"));
        assertThat(transaction.getNote(), is("Rechnung Rückruf/Nachforschung"));
    }

    @Test
    public void testGiroKontoauszug14()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(6L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.64)));
        assertThat(transaction.getSource(), is("GiroKontoauszug14.txt"));
        assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.01.2022 bis 31.03.2022"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-21T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(161.65)));
        assertThat(transaction.getSource(), is("GiroKontoauszug14.txt"));
        assertThat(transaction.getNote(), is("Geldautomat (Fremdwährung)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-22T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(69)));
        assertThat(transaction.getSource(), is("GiroKontoauszug14.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung online"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-25T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.01)));
        assertThat(transaction.getSource(), is("GiroKontoauszug14.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung onl FW"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-09T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(200.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug14.txt"));
        assertThat(transaction.getNote(), is("Geldautomat"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-28T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug14.txt"));
        assertThat(transaction.getNote(), is("Geldautomat"));
    }

    @Test
    public void testGiroKontoauszug15()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug15.txt"), errors);

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
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-08-18T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(13.74)));
        assertThat(transaction.getSource(), is("GiroKontoauszug15.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung online"));
    }

    @Test
    public void testGiroKontoauszug16()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug16.txt"), errors);

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
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-05T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(15.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug16.txt"));
        assertThat(transaction.getNote(), is("Überweisung entgeltfrei"));
    }

    @Test
    public void testGiroKontoauszug17()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug17.txt"), errors);

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
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-08-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug17.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));
    }

    @Test
    public void testGiroKontoauszug18()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug18.txt"), errors);

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
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-03T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(26.91)));
        assertThat(transaction.getSource(), is("GiroKontoauszug18.txt"));
        assertThat(transaction.getNote(), is("Storno Gutschrift"));
    }

    @Test
    public void testGiroKontoauszug19()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug19.txt"), errors);

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
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-06-12T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.01)));
        assertThat(transaction.getSource(), is("GiroKontoauszug19.txt"));
        assertThat(transaction.getNote(), is("Buchung Identifikationscode"));
    }

    @Test
    public void testGiroKontoauszug20()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug20.txt"), errors);

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
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-05T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.80)));
        assertThat(transaction.getSource(), is("GiroKontoauszug20.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-06T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(24.65)));
        assertThat(transaction.getSource(), is("GiroKontoauszug20.txt"));
        assertThat(transaction.getNote(), is("Basislastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-24T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1234.56)));
        assertThat(transaction.getSource(), is("GiroKontoauszug20.txt"));
        assertThat(transaction.getNote(), is("Eingang Inst.Paym."));
    }

    @Test
    public void testGiroKontoauszug21()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-06-22T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(8.97)));
        assertThat(transaction.getSource(), is("GiroKontoauszug21.txt"));
        assertThat(transaction.getNote(), is("KARTENZAHLUNG"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-06-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.14)));
        assertThat(transaction.getSource(), is("GiroKontoauszug21.txt"));
        assertThat(transaction.getNote(), is("KARTENZAHLUNG"));
    }

    @Test
    public void testGiroKontoauszug22()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug22.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(12L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-06T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(50.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug22.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-09T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.50)));
        assertThat(transaction.getSource(), is("GiroKontoauszug22.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-16T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.80)));
        assertThat(transaction.getSource(), is("GiroKontoauszug22.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-18T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(50.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug22.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-23T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(7.50)));
        assertThat(transaction.getSource(), is("GiroKontoauszug22.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-26T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(3.71)));
        assertThat(transaction.getSource(), is("GiroKontoauszug22.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung (Fremdwährung)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.40)));
        assertThat(transaction.getSource(), is("GiroKontoauszug22.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(50.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug22.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-27T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(201.23)));
        assertThat(transaction.getSource(), is("GiroKontoauszug22.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-02-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1600.00)));
        assertThat(transaction.getSource(), is("GiroKontoauszug22.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-18T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(11.88)));
        assertThat(transaction.getSource(), is("GiroKontoauszug22.txt"));
        assertThat(transaction.getNote(), is("sonstige Entgelte Girokarte"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10.12)));
        assertThat(transaction.getSource(), is("GiroKontoauszug22.txt"));
        assertThat(transaction.getNote(), is("sonstige Entgelte Stornorechnung"));
    }

    @Test
    public void testGiroKontoauszug23()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(47L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(47));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-26"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-07-04"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-05"), hasAmount("EUR", 288.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-05"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-05"), hasAmount("EUR", 12.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-06"), hasAmount("EUR", 9.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-06"), hasAmount("EUR", 26.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-07"), hasAmount("EUR", 276.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-07"), hasAmount("EUR", 4.34), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-08"), hasAmount("EUR", 7.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-08"), hasAmount("EUR", 39.90), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-08"), hasAmount("EUR", 33.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 10.55), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 9.90), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 41.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 31.63), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 93.70), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 22.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 30.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-13"), hasAmount("EUR", 29.58), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-14"), hasAmount("EUR", 40.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-15"), hasAmount("EUR", 51.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-15"), hasAmount("EUR", 37.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-16"), hasAmount("EUR", 69.06), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-20"), hasAmount("EUR", 9.80), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-20"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-21"), hasAmount("EUR", 46.37), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-21"), hasAmount("EUR", 41.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-21"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-22"), hasAmount("EUR", 14.83), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-22"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-23"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-26"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-27"), hasAmount("EUR", 17.42), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-27"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-28"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-30"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-03"), hasAmount("EUR", 1185.90), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-03"), hasAmount("EUR", 32.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-03"), hasAmount("EUR", 18.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-03"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-03"), hasAmount("EUR", 52.63), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-04"), hasAmount("EUR", 6.25), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-04"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-07-03"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Entgelt für Konto ohne mtl. Eingang"))));
    }

    @Test
    public void testGiroKontoauszug24()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(26L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(26));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-07"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-07"), hasAmount("EUR", 150.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-07"), hasAmount("EUR", 55.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-07"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-08-07"), hasAmount("EUR", 237.44), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-14"), hasAmount("EUR", 60.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-17"), hasAmount("EUR", 76.05), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-17"), hasAmount("EUR", 29.70), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-18"), hasAmount("EUR", 48.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-18"), hasAmount("EUR", 35.50), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-18"), hasAmount("EUR", 12.75), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-21"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Geldautomat"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-22"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-28"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-28"), hasAmount("EUR", 40.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-29"), hasAmount("EUR", 86.88), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-08-29"), hasAmount("EUR", 800.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-30"), hasAmount("EUR", 9.99), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-08-30"), hasAmount("EUR", 75.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-01"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-01"), hasAmount("EUR", 5.99), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-04"), hasAmount("EUR", 277.97), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-04"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Geldautomat"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-04"), hasAmount("EUR", 62.70), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));
    }

    @Test
    public void testGiroKontoauszug25()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug25.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2023-08-23"), hasAmount("EUR", 200.90), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-28"), hasAmount("EUR", 3.93), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Kartenzahlung (Fremdwährung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-09-01"), hasAmount("EUR", 1900.00), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug26()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug26.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-05"), hasAmount("EUR", 480.00), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-05"), hasAmount("EUR", 20.00), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-06"), hasAmount("EUR", 73.36), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-09-06"), hasAmount("EUR", 9999.99), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-04"), hasAmount("EUR", 150.00), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-10-04"), hasAmount("EUR", 150.00), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2023-10-02"), hasAmount("EUR", 0.01), //
                        hasSource("GiroKontoauszug26.txt"),
                        hasNote("Abrechnungszeitraum vom 01.07.2023 bis 30.09.2023"))));
    }

    @Test
    public void testGiroKontoauszug27()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug27.txt"), errors);

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
        assertThat(results, hasItem(interest( //
                        hasDate("2023-06-30"), hasShares(0), //
                        hasSource("GiroKontoauszug27.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.04.2023 bis 30.06.2023"), //
                        hasAmount("EUR", 222.74), hasGrossValue("EUR", 302.54), //
                        hasTaxes("EUR", (75.64 + 4.16)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug28()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-01-15"), hasAmount("EUR", 11.88), //
                        hasSource("GiroKontoauszug28.txt"), hasNote("sonstige Entgelte Girokarte"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-16"), hasAmount("EUR", 65.28), //
                        hasSource("GiroKontoauszug28.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-17"), hasAmount("EUR", 52.33), //
                        hasSource("GiroKontoauszug28.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-24"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug28.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-26"), hasAmount("EUR", 4.75), //
                        hasSource("GiroKontoauszug28.txt"), hasNote("Kartenzahlung (Fremdwährung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-02-01"), hasAmount("EUR", 2600.00), //
                        hasSource("GiroKontoauszug28.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug29()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug29.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-09"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-09"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-23"), hasAmount("EUR", 390.11), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-23"), hasAmount("EUR", 400.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 110.82), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 8.70), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2024-12-30"), hasAmount("EUR", 0.07), //
                        hasSource("GiroKontoauszug29.txt"),
                        hasNote("Abrechnungszeitraum vom 01.10.2024 bis 31.12.2024"))));
    }

    @Test
    public void testGiroKontoauszug30()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug30.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2025-02-06"), hasAmount("EUR", 460.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Kartenzahlung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-17"), hasAmount("EUR", 44.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Kartenzahlung"))));
    }

    @Test
    public void testGiroKontoauszug31()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug31.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2025-04-01"), hasAmount("EUR", 5.99), //
                        hasSource("GiroKontoauszug31.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2025-04-01"), hasAmount("EUR", 0.04), //
                        hasSource("GiroKontoauszug31.txt"),
                        hasNote("Abrechnungszeitraum vom 01.01.2025 bis 31.03.2025"))));
    }

    @Test
    public void testGiroKontoauszug32()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug32.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2025-04-08"), hasAmount("EUR", 13.20), //
                        hasSource("GiroKontoauszug32.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-05-02"), hasAmount("EUR", 2000.00), //
                        hasSource("GiroKontoauszug32.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug33()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug33.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-07-07"), hasAmount("EUR", 1.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-07-10"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-07-14"), hasAmount("EUR", 65.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-07-14"), hasAmount("EUR", 65.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Eingang Echtzeitüberweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-08-01"), hasAmount("EUR", 7.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-08-01"), hasAmount("EUR", 30.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-08-04"), hasAmount("EUR", 4.99), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Kartenzahlung"))));
    }

    @Test
    public void testGiroKontoauszug34()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug34.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2025-11-06"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug34.txt"), hasNote("Echtzeitüberweisung"))));
    }

    @Test
    public void testTagesgeldKontoauszug01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TagesgeldKontoauszug01.txt"), errors);

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
        assertThat(results, hasItem(interest(hasDate("2024-07-01"), hasAmount("EUR", 11.51), //
                        hasSource("TagesgeldKontoauszug01.txt"),
                        hasNote("Abrechnungszeitraum vom 01.04.2024 bis 30.06.2024"))));
    }

    @Test
    public void testTagesgeldKontoauszug02()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TagesgeldKontoauszug02.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2024-06-13"), hasAmount("EUR", 2000.00), //
                        hasSource("TagesgeldKontoauszug02.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-26"), hasAmount("EUR", 400.00), //
                        hasSource("TagesgeldKontoauszug02.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-27"), hasAmount("EUR", 9180.74), //
                        hasSource("TagesgeldKontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-04"), hasAmount("EUR", 9982.07), //
                        hasSource("TagesgeldKontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-07-01"), hasAmount("EUR", 17.93), //
                        hasSource("TagesgeldKontoauszug02.txt"),
                        hasNote("Abrechnungszeitraum vom 01.04.2024 bis 30.06.2024"))));
    }

    @Test
    public void testTagesgeldKontoauszug03()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TagesgeldKontoauszug03.txt"), errors);

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
        assertThat(results, hasItem(interest( //
                        hasDate("2024-07-01"), hasShares(0), //
                        hasSource("TagesgeldKontoauszug03.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.04.2024 bis 30.06.2024"), //
                        hasAmount("EUR", 56.47), hasGrossValue("EUR", 76.70), //
                        hasTaxes("EUR", (19.18 + 1.05)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesgeldKontoauszug04()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TagesgeldKontoauszug04.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2025-01-02"), hasAmount("EUR", 133.51), //
                        hasSource("TagesgeldKontoauszug04.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-03"), hasAmount("EUR", 406.97), //
                        hasSource("TagesgeldKontoauszug04.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-04"), hasAmount("EUR", 406.99), //
                        hasSource("TagesgeldKontoauszug04.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2025-01-05"), hasAmount("EUR", 0.02), //
                        hasSource("TagesgeldKontoauszug04.txt"), hasNote("Stornorechnung zur Abrechnung 30.12.2024"))));
    }

    @Test
    public void testKreditKontoauszug01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(7L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-09T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-11-23T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(57.57)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("WWW.onlineshop.DE,"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-21T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.01)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Habenzins auf 28 Tage"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-23T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.01)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Abgeltungsteuer"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-25T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(62.32)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Laden Ort 220,"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-25T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(19.36)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Laden GmbH & Co., Ort"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2450)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Auszahlung"));
    }

    @Test
    public void testKreditKontoauszug02()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(8L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-04T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(300.00)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-26T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("DEUTSCHE BANK AG, DD-LOEBTAU"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("VB FlatCity-BAUTZEN EG, DD"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-05T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("VB FlatCity-BAUTZEN EG, DD"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-09T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("VB FlatCity-BAUTZEN EG, DD"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-11T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("VB FlatCity-BAUTZEN EG, DD"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-18T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("DEUTSCHE BANK AG, DD-LOEBTAU"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-18T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.27)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("GITHUB, HTTPSGITHUB.C"));
    }

    @Test
    public void testKreditKontoauszug03()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug03.txt"), errors);

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
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(5L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-21T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(40.72)));
        assertThat(transaction.getSource(), is("KreditKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Ausgleich Kreditkarte gem. Abrechnung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-07T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(400.00)));
        assertThat(transaction.getSource(), is("KreditKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
        assertThat(transaction.getSource(), is("KreditKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("VB DRESDEN-BAUTZEN EG, DD"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-09T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
        assertThat(transaction.getSource(), is("KreditKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("VB DRESDEN-BAUTZEN EG, DD"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-20T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.27)));
        assertThat(transaction.getSource(), is("KreditKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("GITHUB, HTTPSGITHUB.C"));
    }

    @Test
    public void testKreditKontoauszug04()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-18T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.80)));
        assertThat(transaction.getSource(), is("KreditKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("GITHUB, HTTPSGITHUB.C"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-20T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.49)));
        assertThat(transaction.getSource(), is("KreditKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Kartenpreis"));
    }

    @Test
    public void testKreditKontoauszug05()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug05.txt"), errors);

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
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-17T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(5.00)));
        assertThat(transaction.getSource(), is("KreditKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("PIN-Gebühr"));
    }

    @Test
    public void testKreditKontoauszug06()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        // get transactions
        var iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(9L));

        var item = iter.next();

        // assert transaction
        var transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-10-23T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1)));
        assertThat(transaction.getSource(), is("KreditKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("STORNIERUNG"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-11-21T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.23)));
        assertThat(transaction.getSource(), is("KreditKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Habenzins auf 28 Tage"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-10-26T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1234)));
        assertThat(transaction.getSource(), is("KreditKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Auszahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-11-03T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1)));
        assertThat(transaction.getSource(), is("KreditKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("ZAHLUNG1"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-11-04T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.96)));
        assertThat(transaction.getSource(), is("KreditKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("ZAHLUNG2"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-11-05T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(34.94)));
        assertThat(transaction.getSource(), is("KreditKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("ZAHLUNG3"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-11-06T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(89.95)));
        assertThat(transaction.getSource(), is("KreditKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("ZAHLUNG4,"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-11-06T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(29.95)));
        assertThat(transaction.getSource(), is("KreditKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("ZAHLUNG5"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-11-13T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(8.99)));
        assertThat(transaction.getSource(), is("KreditKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("ZAHLUNG6"));
    }

    @Test
    public void testKreditKontoauszug07()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug07.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2024-11-29"), hasAmount("EUR", 392.31), //
                        hasSource("KreditKontoauszug07.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-11-29"), hasAmount("EUR", 500.00), //
                        hasSource("KreditKontoauszug07.txt"), hasNote("PAYPAL *abc, 35314369001"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-23"), hasAmount("EUR", 134.47), //
                        hasSource("KreditKontoauszug07.txt"), hasNote("PAYPAL *abc, 35314369001"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-27"), hasAmount("EUR", 500.00), //
                        hasSource("KreditKontoauszug07.txt"), hasNote("PAYPAL *abc, 35314369001"))));
    }

    @Test
    public void testKreditKontoauszug08()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug08.txt"), errors);

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
        assertThat(results, hasItem(interest(hasDate("2007-07-21"), hasAmount("EUR", 12.54), //
                        hasSource("KreditKontoauszug08.txt"), hasNote("Habenzins auf 23 Tage"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2007-07-21"), hasAmount("EUR", 2.92), //
                        hasSource("KreditKontoauszug08.txt"), hasNote("Habenzins auf 5 Tage"))));
    }

    @Test
    public void testKreditKontoauszug09()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug09.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2007-09-21"), hasAmount("EUR", 500.00), //
                        hasSource("KreditKontoauszug09.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2007-10-11"), hasAmount("EUR", 1017.85), //
                        hasSource("KreditKontoauszug09.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2007-10-23"), hasAmount("EUR", 19.88), //
                        hasSource("KreditKontoauszug09.txt"), hasNote("Habenzins auf 31 Tage"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2007-09-22"), hasAmount("EUR", 0.05), //
                        hasSource("KreditKontoauszug09.txt"), hasNote("Storno Habenzinsen"))));
    }

    @Test
    public void testKreditKontoauszug10()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug10.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2009-01-26"), hasAmount("EUR", 18500.00), //
                        hasSource("KreditKontoauszug10.txt"), hasNote("Auszahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2009-02-03"), hasAmount("EUR", 550.00), //
                        hasSource("KreditKontoauszug10.txt"), hasNote("Auszahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2009-02-04"), hasAmount("EUR", 300.00), //
                        hasSource("KreditKontoauszug10.txt"), hasNote("Auszahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2009-02-20"), hasAmount("EUR", 17700.00), //
                        hasSource("KreditKontoauszug10.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2009-02-21"), hasAmount("EUR", 11.19), //
                        hasSource("KreditKontoauszug10.txt"), hasNote("Habenzins auf 29 Tage"))));
    }
}
