package name.abuchen.portfolio.datatransfer.pdf.justtrade;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.pdf.JustTradePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
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
public class JustTradePDFExtractorTest
{
    private List<Item> results;

    @Before
    public void loadPDF()
    {
        JustTradePDFExtractor extractor = new JustTradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "sutor_umsaetze_pdf.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(13));
    }

    @Test
    public void testSecurities() throws IOException
    {
        List<Security> securities = results.stream().filter(i -> i instanceof SecurityItem)
                        .map(item -> item.getSecurity()).collect(Collectors.toList());

        assertThat(securities.size(), is(3));
        securities.forEach(security -> assertNull(security.getIsin()));

        assertThat(securities.get(0).getName(), is("iShares Core MSCI Emerging Markets"));
        assertThat(securities.get(1).getName(), is("Lyxor Core Stoxx Europe 600 acc"));
        assertThat(securities.get(2).getName(), is("Dimensional European Value Fund"));
    }

    @Test
    public void testDeposits() throws IOException
    {
        List<TransactionItem> transactionItems = results.stream().filter(i -> i instanceof TransactionItem)
                        .map(i -> (TransactionItem) i).collect(Collectors.toList());

        assertThat(transactionItems.size(), is(5));
        transactionItems.forEach(item -> assertThat(item.getAmount().getCurrencyCode(), is("EUR")));

        // direct deposit
        assertThat(transactionItems.get(0).getAmount().getAmount(), is(16042L));
        // Zulage deposit
        assertThat(transactionItems.get(1).getAmount().getAmount(), is(22200L));
        // administration fee
        assertThat(transactionItems.get(2).getAmount().getAmount(), is(1983L));
        // partial administration fee
        assertThat(transactionItems.get(3).getAmount().getAmount(), is(212L));
        // account management fees
        assertThat(transactionItems.get(4).getAmount().getAmount(), is(1350L));
    }

    @Test
    public void testBuyTransactions() throws IOException
    {
        List<BuySellEntry> entries = getTransactionEntries("BUY");

        assertThat(entries.size(), is(3));

        validateTransaction(entries, 0, 8_35L, "2019-07-04T00:00", 0.3308);
        validateTransaction(entries, 1, 242_09L, "2019-07-03T00:00", 1.5264);
        validateTransaction(entries, 2, 75_22L, "2019-07-02T00:00", 6.2631);
    }

    @Test
    public void testSellForFeePayments() throws IOException
    {
        List<BuySellEntry> entries = getTransactionEntries("SELL");

        assertThat(entries.size(), is(2));

        validateTransaction(entries, 0, 98L, "2019-07-05T00:00", 0.0387);
        validateTransaction(entries, 1, 4_78L, "2019-07-06T00:00", 0.0299);
    }

    private List<BuySellEntry> getTransactionEntries(String type)
    {
        return results.stream().filter(i -> i instanceof BuySellEntryItem).map(i -> (BuySellEntryItem) i)
                        .map(i -> (BuySellEntry) i.getSubject())
                        .filter(entry -> entry.getAccountTransaction().getType()
                                        .equals(AccountTransaction.Type.valueOf(type)))
                        .filter(entry -> entry.getPortfolioTransaction().getType()
                                        .equals(PortfolioTransaction.Type.valueOf(type)))
                        .collect(Collectors.toList());

    }

    private void validateTransaction(List<BuySellEntry> entries, int index, long amount, String dateTime, double shares)
    {
        assertThat(entries.get(index).getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, amount)));
        assertThat(entries.get(index).getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse(dateTime)));
        assertThat(entries.get(index).getPortfolioTransaction().getShares(), is(Values.Share.factorize(shares)));
    }

    @Test
    public void testUmsaetze2()
    {
        JustTradePDFExtractor extractor = new JustTradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "sutor_umsaetze_pdf2.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check securities
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("Vanguard EUR Eurozone Gov Bond ETF"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        security = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("Amundi Solution MSCI Europe Min Vol"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        security = results.stream().filter(i -> i instanceof SecurityItem).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("iShares Edge MSCI EM Min Vol ETF"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        security = results.stream().filter(i -> i instanceof SecurityItem).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("Xtrackers MSCI World Min Vol ETF"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check first transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(719.05))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(26.7524)));

        // check 2nd transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(145.32))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.4517)));

        // check 3rd transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(2).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(48.44))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.9784)));

        // check 4th transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(3).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.97))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(34.7017)));

        // check 5th transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(4).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(683.50))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25.3618)));

        // check account transactions (fees, deposits, etc.)
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(175.00))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2020-03-02T00:00")));

        // check 2nd transaction
        t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(1).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.FEES));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.00))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2020-06-11T00:00")));

        // check 3rd transaction
        t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(2).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.FEES));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.92))));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2020-01-08T00:00")));
    }

    @Test
    public void testUmsaetze3()
    {
        JustTradePDFExtractor extractor = new JustTradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "sutor_umsaetze_pdf3.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(70));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check securities
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("Xtr II iBoxx Euroz Gov Bd 1-3 ETF"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check first transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.83))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.0405)));
    }

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
        assertThat(security.getName(), is("ISHSII-DEV.MKTS PROP.YLD U.ETF REGISTERED SHS USD (DIST) O.N."));

        // check transaction
        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        BuySellEntry entry = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();
        PortfolioTransaction tx = entry.getPortfolioTransaction();

        assertThat(tx.getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(tx.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1340.64))));
        assertThat(tx.getDateTime(), is(LocalDateTime.parse("2020-01-02T10:49:34")));
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

    @Test
    public void testDividende01()
    {
        Client client = new Client();

        JustTradePDFExtractor extractor = new JustTradePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("US4878361082"));
        assertThat(security.getName(), is("Kellogg Co."));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(12.15))));
        assertThat(t.getShares(), is(Values.Share.factorize(30)));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2021-03-01T00:00")));

        assertThat(t.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(14.29))));
        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(2.14))));
        assertThat(t.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }
}
