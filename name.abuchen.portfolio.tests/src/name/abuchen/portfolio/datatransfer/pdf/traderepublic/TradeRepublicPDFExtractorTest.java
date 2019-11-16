package name.abuchen.portfolio.datatransfer.pdf.traderepublic;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TradeRepublicPDFExtractor;
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
public class TradeRepublicPDFExtractorTest
{
    @Test
    public void testKauf01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00BKM4GZ66"));
        assertThat(security.getName(), is("iShs Core MSCI EM IMI U.ETF"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.05))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2019-05-13T12:14")));
        assertThat(tx.getShares(), is(Values.Share.factorize(1)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
    }

    @Test
    public void testKauf02()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US08862E1091"));
        assertThat(security.getName(), is("Beyond Meat Inc."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(59.19))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2019-05-13T13:59")));
        assertThat(tx.getShares(), is(Values.Share.factorize(1)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
    }

    @Test
    public void testKauf03()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US88160R1014"));
        assertThat(security.getName(), is("Tesla Inc."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(193.08))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2019-06-17T12:27")));
        assertThat(tx.getShares(), is(Values.Share.factorize(1)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
    }

    @Test
    public void testVerkauf01()
    {
        TradeRepublicPDFExtractor extractor = new TradeRepublicPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("AU000000CUV3"));
        assertThat(security.getName(), is("Clinuvel Pharmaceuticals Ltd."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1792.29))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2019-06-18T17:50")));
        assertThat(tx.getShares(), is(Values.Share.factorize(80)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
        assertThat(tx.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30.63 + 1.68))));
    }

}
