package name.abuchen.portfolio.datatransfer.pdf.fidelity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.FidelityPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.FidelityStockPlanPDFExtractor;
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
public class FidelityPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        FidelityPDFExtractor extractor = new FidelityPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "Fidelity-TradeConfirmation01.txt"), errors);

        assertThat(errors, empty());
        /*
         * System.out.println("Got " + results.size() + " results"); for (Item
         * item : results) { System.out.println("item: " + item); }
         */
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check first security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("SLV"));
        assertThat(security.getName(), is("ISHARES SILVER TR ISHARES"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(40)));
        assertThat(entry.getSource(), is("Fidelity-TradeConfirmation01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1011.60))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1011.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check second security
        security = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("IWB"));
        assertThat(security.getName(), is("ISHARES RUSSELL 1000 INDEX FUND"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(24)));
        assertThat(entry.getSource(), is("Fidelity-TradeConfirmation01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4994.40))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4994.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check third security
        security = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("EFG"));
        assertThat(security.getName(), is("ISHARES TR EAFE GRWTH ETF"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));
        assertThat(entry.getSource(), is("Fidelity-TradeConfirmation01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5082.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5082.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check fourth security
        security = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("DGRO"));
        assertThat(security.getName(), is("ISHARES TRUST CORE DIVID GWTH"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(114)));
        assertThat(entry.getSource(), is("Fidelity-TradeConfirmation01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5016.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5016.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check fifth security
        security = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("BGRN"));
        assertThat(security.getName(), is("ISHARES TR GBL GREEN ETF"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-04T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(35)));
        assertThat(entry.getSource(), is("Fidelity-TradeConfirmation01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1969.45))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1969.45))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        FidelityStockPlanPDFExtractor extractor = new FidelityStockPlanPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "Fidelity-TradeConfirmation02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check first security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("AAPL"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-12-12T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14)));
        assertThat(entry.getSource(), is("Fidelity-TradeConfirmation02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(15023.41))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(15023.54))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.13))));
    }

    @Test
    public void testWertpapierKaufVerkauf03()
    {

        FidelityPDFExtractor extractor = new FidelityPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "Fidelity-TradeConfirmation03.txt"), errors);

        // if (!errors.isEmpty())
        // {
        // for (Exception ex : errors)
        // {
        // System.out.println(ex.getMessage());
        // System.out.println(ex.getStackTrace());
        // }
        // }
        //
        // System.out.println("Got " + results.size() + " results");
        // for (Item item : results)
        // {
        // System.out.println("item: " + item);
        // }

        assertThat(errors, empty());
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check first security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("GOOG"));
        assertThat(security.getName(), is("ALPHABET INC CAP STK CL C"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-12-14T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));
        assertThat(entry.getSource(), is("Fidelity-TradeConfirmation03.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(3990.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(3990.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check second security
        security = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("BAM"));
        assertThat(security.getName(), is("BROOKFIELD ASSET MANAGEMENT LTD CLASS"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-12-14T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(203)));
        assertThat(entry.getSource(), is("Fidelity-TradeConfirmation03.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(7935.20))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(7935.27))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.07))));

        // check buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-12-14T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.032)));
        assertThat(entry.getSource(), is("Fidelity-TradeConfirmation03.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.25))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.25))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        // check third security
        security = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("NTR"));
        assertThat(security.getName(), is("NUTRIEN LTD COM NPV ISIN #CA67077M1086"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-12-14T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(80)));
        assertThat(entry.getSource(), is("Fidelity-TradeConfirmation03.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4427.59))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4427.59))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.USD);
        Status s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }
}
