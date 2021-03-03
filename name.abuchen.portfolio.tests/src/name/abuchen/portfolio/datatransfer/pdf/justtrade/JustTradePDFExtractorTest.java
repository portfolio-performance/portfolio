package name.abuchen.portfolio.datatransfer.pdf.justtrade;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.JustTradePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class JustTradePDFExtractorTest
{

    @Test
    public void testKauf01()
    {
        JustTradePDFExtractor extractor = new JustTradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00B1FZS350"));
        assertThat(security.getWkn(), is("A0LEW8"));
        assertThat(security.getName(), is("ISHSII-DEV.MKTS PROP.YLD U.ETF"));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1340.64))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2020-01-02T00:00")));
        assertThat(tx.getShares(), is(Values.Share.factorize(53)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0))));
    }

    @Test
    public void testKauf02()
    {
        JustTradePDFExtractor extractor = new JustTradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00BK5BQT80"));
        assertThat(security.getName(), is("Vanguard FTSE All-World U.ETF Re"));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2083.94))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2020-06-03T09:27:01")));
        assertThat(tx.getShares(), is(Values.Share.factorize(29)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0))));
    }

    @Test
    public void testKauf03()
    {
        JustTradePDFExtractor extractor = new JustTradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE0006069008"));
        assertThat(security.getName(), is("FROSTA AG"));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2292.00))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2021-03-01T18:23:36")));
        assertThat(tx.getShares(), is(Values.Share.factorize(30)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0))));
    }

    @Test
    public void testVerkauf01()
    {
        JustTradePDFExtractor extractor = new JustTradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE000CL9E825"));
        assertThat(security.getName(), is("Leveraged Certificate auf DAX"));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2232.23))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2020-07-31T21:00:15")));
        assertThat(tx.getShares(), is(Values.Share.factorize(58)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0))));
        assertThat(tx.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.77))));
    }

    @Test
    public void testSammelabrechnung01()
    {
        JustTradePDFExtractor extractor = new JustTradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sammelabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE000SR8YZ53"));
        assertThat(security.getName(), is("Leveraged Certificate auf DAX / XDAX COMBI INDEX"));

        

        // check first transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1228.00))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2020-08-10T14:31:04")));
        assertThat(tx.getShares(), is(Values.Share.factorize(100)));
        assertThat(tx.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0))));
        assertThat(tx.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0))));

        // check second transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).skip(1).findFirst();
        entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1575.00))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2020-08-10T15:48:16")));
        assertThat(tx.getShares(), is(Values.Share.factorize(125)));

        // check fifth second transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).skip(4).findFirst();
        entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1262.00))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2020-08-10T15:33:52")));
        assertThat(tx.getShares(), is(Values.Share.factorize(100)));
        
        // check sixth second transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).skip(5).findFirst();
        entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1576.25))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2020-08-10T15:50:52")));
        assertThat(tx.getShares(), is(Values.Share.factorize(125)));
        
    }

}
