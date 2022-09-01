package name.abuchen.portfolio.datatransfer.pdf.ajbellsecuritieslimited;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.AJBellSecuritiesLimitedPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class AJBellSecuritiesLimitedPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        AJBellSecuritiesLimitedPDFExtractor extractor = new AJBellSecuritiesLimitedPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getSedol(), is("BF41Q72"));
        assertThat(security.getName(), is("LEGAL & GENERAL(UNIT TRUST MNGRS) WORLD CLIM CHNGE EQTY FACTORS IND I ACC"));
        assertThat(security.getCurrencyCode(), is("GBP"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-12-05T13:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(17940.965)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Ref.: C5L6DQ"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("GBP", Values.Amount.factorize(10000.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("GBP", Values.Amount.factorize(9998.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("GBP", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("GBP", Values.Amount.factorize(1.50))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        AJBellSecuritiesLimitedPDFExtractor extractor = new AJBellSecuritiesLimitedPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getSedol(), is("BF41Q72"));
        assertThat(security.getName(), is("LEGAL & GENERAL(UNIT TRUST MNGRS) WORLD CLIM CHNGE EQTY FACTORS IND I ACC"));
        assertThat(security.getCurrencyCode(), is("GBP"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-11-08T13:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14008.055)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Ref.: C46TWX"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("GBP", Values.Amount.factorize(8001.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("GBP", Values.Amount.factorize(8000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("GBP", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("GBP", Values.Amount.factorize(1.50))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        AJBellSecuritiesLimitedPDFExtractor extractor = new AJBellSecuritiesLimitedPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getSedol(), is("BDGSVH2"));
        assertThat(security.getName(), is("XTRACKERS (IE) PLC MSCI WLD INFO TECHNOLOGY UCITS ETF 1C"));
        assertThat(security.getCurrencyCode(), is("GBP"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-12-05T10:22")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(380)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Ref.: C5L6BV"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("GBP", Values.Amount.factorize(9980.06))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("GBP", Values.Amount.factorize(9970.11))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("GBP", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("GBP", Values.Amount.factorize(9.95))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        AJBellSecuritiesLimitedPDFExtractor extractor = new AJBellSecuritiesLimitedPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getSedol(), is("B8NZ739"));
        assertThat(security.getName(), is("UBS (LUX) FUND SOLUTIONS MSCI WORLD SOC RSPON A UCITS USD DIS"));
        assertThat(security.getCurrencyCode(), is("GBP"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-06-28T08:10")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(159)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Ref.: CNG7G1"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("GBP", Values.Amount.factorize(15783.20))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("GBP", Values.Amount.factorize(15793.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("GBP", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("GBP", Values.Amount.factorize(9.95))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        AJBellSecuritiesLimitedPDFExtractor extractor = new AJBellSecuritiesLimitedPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "GBP");

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getSedol(), is("B7NLLS3"));
        assertThat(security.getName(), is("VANGUARD FUNDS PLC S&P 500 UCITS E T F INC NAV GBP"));
        assertThat(security.getCurrencyCode(), is("GBP"));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-08-17T09:01")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(470)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Ref.: C8M779"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("GBP", Values.Amount.factorize(31643.75))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("GBP", Values.Amount.factorize(31653.70))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("GBP", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("GBP", Values.Amount.factorize(9.95))));
    }
}
