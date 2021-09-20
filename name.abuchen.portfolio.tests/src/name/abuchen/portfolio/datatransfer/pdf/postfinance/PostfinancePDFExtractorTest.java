package name.abuchen.portfolio.datatransfer.pdf.postfinance;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.PostfinancePDFExtractor;
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
public class PostfinancePDFExtractorTest
{
    @Test
    public void testKauf01()
    {
        Client client = new Client();
        
        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);
        
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceKauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("NL0000009355"));
        assertThat(security.getCurrencyCode(), is("EUR"));
        assertThat(security.getName(), is("UNILEVER DUTCH CERT"));
        
        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-09-27T00:00")));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(2850.24))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(8.58))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(4.26))));
    }

    @Test
    public void testKauf02()
    {
        Client client = new Client();
        
        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);
        
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceKauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("CH0025751329"));
        assertThat(security.getCurrencyCode(), is("CHF"));
        assertThat(security.getName(), is("LOGITECH N"));
        
        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-10-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(2998.95))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(26.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(2.25))));
    }
    
    @Test
    public void testKauf03()
    {
        Client client = new Client();
        
        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);
        
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceKauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("DE000PAH0038"));
        assertThat(security.getCurrencyCode(), is("EUR"));
        assertThat(security.getName(), is("PORSCHE AUTOMOBIL HOLDING PRF"));
        
        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-03-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(2968.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(4.45))));
        
        Unit gross = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(gross.getAmount(), is(Money.of("CHF", Values.Amount.factorize(2964.05))));
        assertThat(gross.getForex(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2737.40))));
    }
    
    @Test
    public void testVerkauf01()
    {
        Client client = new Client();
        
        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);
        
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceVerkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("IE00BD4TYL27"));
        assertThat(security.getCurrencyCode(), is("CHF"));
        assertThat(security.getName(), is("UBSETF MSCI USA hdg to CHF"));
        
        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-09-24T00:00")));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(7467.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(2.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(11.20))));
    }
    
    @Test
    public void testDividende01()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceDividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("NL0000009355"));
        assertThat(security.getName(), is("UNILEVER DUTCH CERT"));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(20.93))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(60)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-02T00:00")));

        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(3.69))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(24.62))));
    }

    @Test
    public void testDividende02()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceDividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("CH0032912732"));
        assertThat(security.getName(), is("UBS ETF CH - SLI CHF A"));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(36.69))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(34)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-09-06T00:00")));

        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("CHF", Values.Amount.factorize(19.75))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(56.44))));
    }
    
    @Test
    public void testKapitalgewinn01()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceKapitalgewinn01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("CH0019396990"));
        assertThat(security.getName(), is("YPSOMED HLDG"));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(26.60))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(19)));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-07-03T00:00")));
    }
    
    @Test
    public void testJahresgebuhr01()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceJahresgebuhr01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(90.00))));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-01-03T00:00")));
    }
    
    @Test
    public void testZinsabschluss01()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceZinsabschluss01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");
       
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(4.59))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-31T00:00")));
    }
    
    @Test
    public void testZinsabschluss02()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceZinsabschluss02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");
       
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(116.66))));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(75.83))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-31T00:00")));
        
        Optional<Unit> unit = transaction.getUnit(Unit.Type.TAX);
        assertThat("Expect tax unit", unit.isPresent());
        assertThat(unit.get().getAmount(), is(Money.of("CHF", Values.Amount.factorize(40.83))));
    }
    
    @Test
    public void testZinsabschlussBefore2018_01()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        //test format used before 2018
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceZinsabschlussBefore2018_01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");
       
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1.00))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-12-31T00:00")));
    }
    
    @Test
    public void testZinsabschlussBefore2018_02()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        //test format used before 2018
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceZinsabschlussBefore2018_02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");
       
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(400.00))));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(260.00))));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-12-31T00:00")));

        Optional<Unit> unit = transaction.getUnit(Unit.Type.TAX);
        assertThat("Expect tax unit", unit.isPresent());
        assertThat(unit.get().getAmount(), is(Money.of("CHF", Values.Amount.factorize(140.00))));
    }
    
    @Test
    public void testKontoauszugGiroBefore2018_01()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        //Capture formatting used before 04/2018
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceKontoauszugGiroBefore2018_01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(54));
        new AssertImportActions().check(results, "CHF");
       
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(54L));

        //removals
        
        //E-FINANCE 00-000000-0
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 250.00, "2015-04-01T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 1000.00, "2015-04-02T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 350.00, "2015-04-02T00:00");
        
        //AUFTRAG DEBIT DIRECT
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 250.00, "2015-04-03T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 250.00, "2015-04-04T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 1150.00, "2015-04-04T00:00");

        //KAUF/ONLINE-SHOPPING
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 200.00, "2015-04-05T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2015-04-06T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 150.00, "2015-04-06T00:00");

        //KAUF/ONLINE-SHOPPING
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 150.00, "2015-04-07T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2015-04-08T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 200.00, "2015-04-08T00:00");
        
        //BARGELDBEZUG
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 50.00, "2015-04-09T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2015-04-10T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 150.00, "2015-04-10T00:00");
        
        //ONLINE-SHOPPING
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 50.00, "2015-04-11T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 75.00, "2015-04-12T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 25.00, "2015-04-12T00:00");

        //KAUF/DIENSTLEISTUNG
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2015-04-13T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 50.00, "2015-04-14T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2015-04-14T00:00");
      
        //AUFTRAG CH-DD-BASISLASTSCHRIFT
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2015-04-15T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 200.00, "2015-04-16T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 300.00, "2015-04-16T00:00");
        
        //GIRO INTERNATIONAL (SEPA)
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2015-04-17T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 150.00, "2015-04-18T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 250.00, "2015-04-18T00:00");
        
        //deposits

        //GIRO AUSLAND
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 100.00, "2015-04-22T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 150.00, "2015-04-22T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 250.00, "2015-04-22T00:00");
        
        //GIRO AUS ONLINE-SIC 000|0000
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 1300.00, "2015-04-22T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 2000.00, "2015-04-22T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 250.00, "2015-04-22T00:00");

        //GIRO AUS KONTO
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 1000.00, "2015-04-23T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 500.00, "2015-04-23T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 250.00, "2015-04-23T00:00");
        
        //GUTSCHRIFT ONLINE-SHOPPING
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 100.00, "2015-04-24T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 500.00, "2015-04-24T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 250.00, "2015-04-24T00:00");

        //GUTSCHRIFT VON FREMDBANK 000|0000
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 100.00, "2015-04-25T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 500.00, "2015-04-25T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 1250.00, "2015-04-25T00:00");

        //GUTSCHRIFT VON FREMDBANK
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 100.00, "2015-04-26T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 500.00, "2015-04-26T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 250.00, "2015-04-26T00:00");

        //transfer
        
        //ÜBERTRAG
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2015-04-27T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 150.00, "2015-04-27T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 250.00, "2015-04-27T00:00");
        
        //fees
        
        //FÜR GIRO INTERNATIONAL (SEPA) > 0
        assertAccountTransaction(iter, AccountTransaction.Type.FEES, 1.00, "2015-04-30T00:00");

        //PREIS FÜR DIE KONTOFÜHRUNG > 0
        assertAccountTransaction(iter, AccountTransaction.Type.FEES, 2.00, "2015-04-30T00:00");
        
        //FÜR DIE KONTOFÜHRUNG > 0
        assertAccountTransaction(iter, AccountTransaction.Type.FEES, 5.00, "2015-04-30T00:00");
        
        //JAHRESPREIS LOGIN
        assertAccountTransaction(iter, AccountTransaction.Type.FEES, 4.00, "2015-04-30T00:00");
        
        //FÜR KONTOAUSZUG PAPIER
        assertAccountTransaction(iter, AccountTransaction.Type.FEES, 24.00, "2015-04-30T00:00");
       
        //interest
        
        //ZINSABSCHLUSS > 0
        assertAccountTransaction(iter, AccountTransaction.Type.INTEREST, 1.00, "2015-04-30T00:00");
        
        assertThat("No additional bookings found", !iter.hasNext());
    }
    
    @Test
    public void testKontoauszugGiro01()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        //Capture formatting used since 04/2018
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceKontoauszugGiro01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(60));
        new AssertImportActions().check(results, "CHF");
       
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(60L));

        //removals
        
        //AUFTRAG C H-D D-B ASISLASTSCHRIFT
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2018-04-01T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 150.00, "2018-04-01T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 250.00, "2018-04-01T00:00");
        
        //ESR
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 250.00, "2018-04-02T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 150.00, "2018-04-02T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2018-04-02T00:00");
        
        //KAUF/DIENSTLEISTUNG
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 25.00, "2018-04-03T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 15.00, "2018-04-03T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 10.00, "2018-04-03T00:00");
        
        //ÜBERTRAG A UF K ONTO
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 2000.00, "2018-04-04T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 1000.00, "2018-04-04T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 500.00, "2018-04-04T00:00");

        //GIRO P OST
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 1300.00, "2018-04-05T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 150.00, "2018-04-05T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 200.00, "2018-04-05T00:00");
        
        //GIRO B ANK
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 1000.00, "2018-04-06T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2018-04-06T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 200.00, "2018-04-06T00:00");
        
        //KAUF/ONLINE S HOPPING
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 10.00, "2018-04-07T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 15.00, "2018-04-07T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 25.00, "2018-04-07T00:00");

        //BARGELDBEZUG V OM 0 0.00.0000
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 150.00, "2018-04-07T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2018-04-07T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2018-04-07T00:00");
        
        //BARGELDBEZUG
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2018-04-08T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 150.00, "2018-04-08T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 250.00, "2018-04-08T00:00");

        //KAUF/DIENSTLEISTUNG VOM 00.00.0000
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2018-04-08T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 150.00, "2018-04-08T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 250.00, "2018-04-08T00:00");
        
        //KAUF/DIENSTLEISTUNG V OM 0 0.00.0000
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 100.00, "2018-04-08T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 150.00, "2018-04-08T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 250.00, "2018-04-08T00:00");
        
        //KAUF/ONLINE-SHOPPING VOM 00.00.0000
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 10.00, "2018-04-09T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 15.00, "2018-04-09T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 25.00, "2018-04-09T00:00");
        
        //KAUF/ONLINE-SHOPPING V OM 0 0.00.0000
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 10.00, "2018-04-09T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 15.00, "2018-04-09T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 25.00, "2018-04-09T00:00");
        
        //TWINT GELD SENDEN
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 10.00, "2018-04-10T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 15.00, "2018-04-10T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 25.00, "2018-04-10T00:00");
        
        //TWINT G ELD S ENDEN
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 10.00, "2018-04-10T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 15.00, "2018-04-10T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 25.00, "2018-04-10T00:00");
        
        //TWINT KAUF/DIENSTLEISTUNG
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 10.00, "2018-04-10T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 15.00, "2018-04-10T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 25.00, "2018-04-10T00:00");
        
        //deposits
        
        //GUTSCHRIFT V ON F REMDBANK
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 10000.00, "2018-04-20T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 600.00, "2018-04-20T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 500.00, "2018-04-20T00:00");
        
        //A EINZAHLUNGSSCHEIN/QR-ZAHLTEIL
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 50.00, "2018-04-21T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 150.00, "2018-04-21T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 250.00, "2018-04-21T00:00");

        //GUTSCHRIFT ONLINE SHOPPING
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 50.00, "2018-04-22T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 150.00, "2018-04-22T00:00");
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 250.00, "2018-04-22T00:00");
        
        //fees
        
        //PREIS F ÜR D IE K ONTOFÜHRUNG > 0
        assertAccountTransaction(iter, AccountTransaction.Type.FEES, 5.00, "2018-04-30T00:00");
        
        //PREIS FÜR EINZAHLUNGEN AM SCHALTER  
        assertAccountTransaction(iter, AccountTransaction.Type.FEES, 2.40, "2018-04-30T00:00");

        //interest
        
        //ZINSABSCHLUSS > 0
        assertAccountTransaction(iter, AccountTransaction.Type.INTEREST, 1.00, "2018-04-30T00:00");
        
        assertThat("No additional bookings found", !iter.hasNext());
    }
    
    @Test
    public void testKontoauszugGiro02()
    {
        Client client = new Client();

        PostfinancePDFExtractor extractor = new PostfinancePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "PostfinanceKontoauszugGiro02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "CHF");
        
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(6L));

        //ÜBERTRAG AUF KONTO
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 20000.00, "2018-04-09T00:00");
        
        //ÜBERTRAG A UF K ONTO
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 20000.00, "2018-04-09T00:00");
        
        //ÜBERTRAG AUS KONTO
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 25000.00, "2018-04-10T00:00");
        
        //ÜBERTRAG A US K ONTO
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 25000.00, "2018-04-11T00:00");
        
        //ÜBERTRAG
        //AUF KONTO 00-00000-0
        assertAccountTransaction(iter, AccountTransaction.Type.REMOVAL, 40000.00, "2018-04-12T00:00");
        
        //ÜBERTRAG
        //AUS KONTO 00-00000-0
        assertAccountTransaction(iter, AccountTransaction.Type.DEPOSIT, 10000.00, "2018-04-13T00:00");

        assertThat("No additional bookings found", !iter.hasNext());
    }
    
    private static void assertAccountTransaction(Iterator<Item> iter, AccountTransaction.Type transactionType, double value, String dateTime)
    {
        assertThat("Expected transaction to exist", iter.hasNext());
        
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        String itemDescription = "Transaction: " + transaction.getMonetaryAmount() + " " + transaction.getDateTime().toString();
        assertThat(itemDescription, transaction.getType(), is(transactionType));
        assertThat(itemDescription, transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(value))));
        assertThat(itemDescription, transaction.getDateTime(), is(LocalDateTime.parse(dateTime)));
    }
}
