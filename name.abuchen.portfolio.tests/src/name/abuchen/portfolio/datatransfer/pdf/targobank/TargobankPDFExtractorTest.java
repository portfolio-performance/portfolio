package name.abuchen.portfolio.datatransfer.pdf.targobank;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TargobankPDFExtractor;
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
public class TargobankPDFExtractorTest
{
    public void runWertpapierOrderTest(String testCaseFilename, int numberOfMatchingFiles, String actualShareName,
                    String actualWkn, String actualIsin, Object actualPortfolioTransactionType,
                    Object actualAccoutTransactionType, String actualDateTime, double actualAmount,
                    String actualCurrency, double actualShares, double actualFees)
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), testCaseFilename), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(numberOfMatchingFiles));

        Optional<Item> securityItem;
        Optional<Item> entryItem;

        // SecurityItem
        securityItem = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(securityItem.isPresent(), is(true));
        Security security = ((SecurityItem) securityItem.get()).getSecurity();
        assertThat(security.getName(), is(actualShareName));
        assertThat(security.getWkn(), is(actualWkn));
        assertThat(security.getIsin(), is(actualIsin));

        // BuySellEntryItem
        entryItem = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(entryItem.isPresent(), is(true));
        assertThat(entryItem.get().getSubject(), instanceOf(BuySellEntry.class));

        // BuySellEntry...
        BuySellEntry entry = (BuySellEntry) entryItem.get().getSubject();
        // ... has the correct type
        assertThat(entry.getPortfolioTransaction().getType(), is(actualPortfolioTransactionType));
        assertThat(entry.getAccountTransaction().getType(), is(actualAccoutTransactionType));
        // ... has the correct values
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse(actualDateTime)));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(actualAmount)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(actualCurrency, Values.Amount.factorize(actualFees))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(actualShares)));
    }

    @Test
    public void testWertpapierKauf01()
    {
        String testCaseFilename = "Kauf_01_(WPX007).txt";
        int numberOfMatchingFiles = 2;
        String actualShareName = "FanCy shaRe. nAmE X0-X0";
        String actualWkn = "ABC123";
        String actualIsin = "DE0000ABC123";
        Object actualPortfolioTransactionType = PortfolioTransaction.Type.BUY;
        Object actualAccountTransactionType = AccountTransaction.Type.BUY;
        String actualDateTime = "2020-01-02T13:01:00";
        double actualAmount = 1008.91;
        String actualCurrency = "EUR";
        double actualShares = 987.654;
        double actualFees = 8.9;
        runWertpapierOrderTest(testCaseFilename, numberOfMatchingFiles, actualShareName, actualWkn, actualIsin,
                        actualPortfolioTransactionType, actualAccountTransactionType, actualDateTime, actualAmount,
                        actualCurrency, actualShares, actualFees);
    }

    @Test
    public void testWertpapierKauf02()
    {
        String testCaseFilename = "Kauf_02_(WPX007).txt";
        int numberOfMatchingFiles = 2;
        String actualShareName = "Muster AG";
        String actualWkn = "ABC123";
        String actualIsin = "DE0000ABC123";
        Object actualPortfolioTransactionType = PortfolioTransaction.Type.BUY;
        Object actualAccountTransactionType = AccountTransaction.Type.BUY;
        String actualDateTime = "2020-09-21T19:27:00";
        double actualAmount = 1187.94;
        String actualCurrency = "EUR";
        double actualShares = 1710;
        double actualFees = 0.0;
        runWertpapierOrderTest(testCaseFilename, numberOfMatchingFiles, actualShareName, actualWkn, actualIsin,
                        actualPortfolioTransactionType, actualAccountTransactionType, actualDateTime, actualAmount,
                        actualCurrency, actualShares, actualFees);
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        String testCaseFilename = "Verkauf_01_(WPX010) TARGO 000000000101753165 am 2020-01-22.txt";
        int numberOfMatchingFiles = 2;
        String actualShareName = "an0tHer vERy FNcY NaMe";
        String actualWkn = "ZYX987";
        String actualIsin = "LU0000ZYX987";
        Object actualPortfolioTransactionType = PortfolioTransaction.Type.SELL;
        Object actualAccountTransactionType = AccountTransaction.Type.SELL;
        String actualDateTime = "2020-01-10T00:00:00";
        double actualAmount = 1239;
        String actualCurrency = "EUR";
        double actualShares = 10;
        double actualFees = 0;
        runWertpapierOrderTest(testCaseFilename, numberOfMatchingFiles, actualShareName, actualWkn, actualIsin,
                        actualPortfolioTransactionType, actualAccountTransactionType, actualDateTime, actualAmount,
                        actualCurrency, actualShares, actualFees);
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "Verkauf_02_(WPX010) vom 2020-05-27.txt",
                                        "Verkauf_02_(WPX011)_Steuerbeilage vom 2020-05-27.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("SE0006425815"));
        assertThat(security.getName(), is("PowerCell Sweden AB (publ) - Namn-Aktier SK-,022"));
        assertThat(security.getWkn(), is("A14TK6"));

        // BuySellEntryItem
        Optional<Item> entryItem = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(entryItem.isPresent(), is(true));

        // BuySellEntry...
        BuySellEntry entry = (BuySellEntry) entryItem.get().getSubject();
        // ... has the correct type
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        // ... has the correct values
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-26T20:32")));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(6615.59)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.65))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(823.76))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(300)));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));

    }

    @Test
    public void testWertpapierVerkauf02SellDocOnly()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Verkauf_02_(WPX010) vom 2020-05-27.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("SE0006425815"));
        assertThat(security.getName(), is("PowerCell Sweden AB (publ) - Namn-Aktier SK-,022"));
        assertThat(security.getWkn(), is("A14TK6"));

        // BuySellEntryItem
        Optional<Item> entryItem = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(entryItem.isPresent(), is(true));

        // BuySellEntry...
        BuySellEntry entry = (BuySellEntry) entryItem.get().getSubject();
        // ... has the correct type
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        // ... has the correct values
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-26T20:32")));
        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(7439.35)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.65))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(300)));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));

    }

    @Test
    public void testDividende01()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Ertragsgutschrift_01_Ertragsgutschrift_(WPX024).txt",
                                        "Ertragsgutschrift_01_Steuerbeilage_(WPX040).txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00BKX55T58"));
        assertThat(security.getName(), is("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN"));
        assertThat(security.getWkn(), is("A12CX1"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-24T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(15.59)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(81)));
        assertThat(transaction.getNote(), is(
                        "Ertragsgutschrift_01_Ertragsgutschrift_(WPX024).txt; Ertragsgutschrift_01_Steuerbeilage_(WPX040).txt"));

        Unit grossValue = transaction.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValue.getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.18))));
        assertThat(grossValue.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(23.77))));

        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(5.59)));
    }

    @Test
    public void testDividende01TaxDocOnly()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Ertragsgutschrift_01_Steuerbeilage_(WPX040).txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00BKX55T58"));
        assertThat(security.getName(), is("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN"));
        assertThat(security.getWkn(), is("A12CX1"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-24T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(15.59)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(81)));
        assertThat(transaction.getNote(), is("Ertragsgutschrift_01_Steuerbeilage_(WPX040).txt"));
        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(5.59)));
    }

    @Test
    public void testDividende01WithSecurityInEuro()
    {
        Client client = new Client();
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        Security existingSecurity = new Security("Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN",
                        CurrencyUnit.EUR);
        existingSecurity.setIsin("IE00BKX55T58");
        existingSecurity.setWkn("A12CX1");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Ertragsgutschrift_01_Ertragsgutschrift_(WPX024).txt",
                                        "Ertragsgutschrift_01_Steuerbeilage_(WPX040).txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-24T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(15.59)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(81)));

        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(5.59)));
    }

    @Test
    public void testDividende02()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Ertragsgutschrift_02_Ertragsgutschrift_(WPX024).txt",
                                        "Ertragsgutschrift_02_Steuerbeilage_(WPX040).txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00BKX55S42"));
        assertThat(security.getName(), is("Vang.FTSE Dev.Eur.ex UK U.ETF - Registered Shares EUR Dis. o"));
        assertThat(security.getWkn(), is("A12CXZ"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-24T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(15.29)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(61)));

        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(0.0)));
    }

    @Test
    public void testDividende02DivDocOnly()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Ertragsgutschrift_02_Ertragsgutschrift_(WPX024).txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00BKX55S42"));
        assertThat(security.getName(), is("Vang.FTSE Dev.Eur.ex UK U.ETF - Registered Shares EUR Dis. o"));
        assertThat(security.getWkn(), is("A12CXZ"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-24T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(15.29)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(61)));

        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(0.0)));
    }

    @Test
    public void testDividende02TaxDocOnly()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Ertragsgutschrift_02_Steuerbeilage_(WPX040).txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00BKX55S42"));
        assertThat(security.getName(), is("Vang.FTSE Dev.Eur.ex UK U.ETF - Registered Shares EUR Dis. o"));
        assertThat(security.getWkn(), is("A12CXZ"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        // expect the "wrong" document date here, because tax is 0.00 and
        // nothing is booked
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-26T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(15.29)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(61)));

        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(0.0)));
    }

    @Test
    public void testDividende03()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Ertragsgutschrift_03_Ertragsgutschrift_(WPX024).txt",
                                        "Ertragsgutschrift_03_Steuerbeilage_(WPX040).txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("LU0875160326"));
        assertThat(security.getName(), is("Xtrackers Harvest CSI300 - Inhaber-Anteile 1D o.N."));
        assertThat(security.getWkn(), is("DBX0NK"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-27T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(228.01)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1700)));

        // gross value 304,81 USD
        Unit grossValue = transaction.getUnit(Unit.Type.GROSS_VALUE).get();
        assertThat(grossValue.getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(279.64))));
        assertThat(grossValue.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(304.81))));

        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(51.63)));
    }

    @Test
    public void testDividende04()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Ertragsgutschrift_04_Dividendengutschrift (WPX020).txt",
                                        "Ertragsgutschrift_04_Steuerbeilage (WPX040).txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE0123456789"));
        assertThat(security.getName(), is("Aktiengesellschaft AG"));
        assertThat(security.getWkn(), is("ABC0DE"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-07-10T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(17.90)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1790)));
    }

    @Test
    public void testDividende05()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Ertragsgutschrift_05_Dividendengutschrift (WPX020).txt",
                                        "Ertragsgutschrift_05_Steuerbeilage (WPX040).txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE0123456789"));
        assertThat(security.getName(), is("Aktiengesellschaft AG"));
        assertThat(security.getWkn(), is("ABC0DE"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-31T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(20.82)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(235)));

        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(3.67)));
    }
    
    @Test
    public void testDividende05WithSecurityInEuro()
    {
        Client client = new Client();
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(client);

        Security existingSecurity = new Security("Aktiengesellschaft AG",
                        CurrencyUnit.EUR);
        existingSecurity.setIsin("DE0123456789");
        existingSecurity.setWkn("ABC0DE");
        client.addSecurity(existingSecurity);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Ertragsgutschrift_05_Dividendengutschrift (WPX020).txt",
                                        "Ertragsgutschrift_05_Steuerbeilage (WPX040).txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(20.82)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(235)));

        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(3.67)));
    }



    @Test
    public void testDividende05DivDocOnly()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Ertragsgutschrift_05_Dividendengutschrift (WPX020).txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE0123456789"));
        assertThat(security.getName(), is("Aktiengesellschaft AG"));
        assertThat(security.getWkn(), is("ABC0DE"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-31T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(20.82)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(235)));

        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(3.67)));
    }

    @Test
    public void testDividende05TaxDocOnly()
    {
        TargobankPDFExtractor extractor = new TargobankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Ertragsgutschrift_05_Steuerbeilage (WPX040).txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();

        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE0123456789"));
        assertThat(security.getName(), is("Aktiengesellschaft AG"));
        assertThat(security.getWkn(), is("ABC0DE"));

        List<AccountTransaction> items = results.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .map(i -> (AccountTransaction) i.getSubject()).collect(Collectors.toList());
        assertThat(items.size(), is(1));

        // dividend
        Optional<AccountTransaction> oTransaction = items.stream()
                        .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType())).findFirst();
        assertThat(oTransaction.isPresent(), is(true));
        AccountTransaction transaction = oTransaction.orElseThrow(IllegalArgumentException::new);

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));

        // expect the "wrong" document date here, because tax is 0.00 and
        // nothing is booked
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-01T00:00")));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(20.82)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(235)));

        assertThat(transaction.getUnitSum(Unit.Type.TAX).getAmount(), is(Values.Amount.factorize(3.67)));
    }


}
