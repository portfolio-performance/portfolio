package name.abuchen.portfolio.datatransfer.pdf.degiro;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

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
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.DegiroPDFExtractor;
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
public class DegiroPDFExtractorTest
{

    @Test
    public void testSanityCheckForBankName()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.createTestCase("some.pdf", "some text"), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), instanceOf(UnsupportedOperationException.class));
    }
    
    private Security assertSecurityKontoauszug3First(Security security)
    {
        assertThat(security.getIsin(), is("DE0002635299"));
        assertThat(security.getName(), is("ISH.S.EU.SEL.DIV.30 U.ETF"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }
    
    private Security assertSecurityKontoauszug3Second(Security security)
    {
        assertThat(security.getIsin(), is("DE000A0F5UH1"));
        assertThat(security.getName(), is("IS.S.GL.SE.D.100 U.ETF A"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }
    
    
    private Security assertSecurityTransaktionsuebersicht2First(Security security)
    {
        assertThat(security.getIsin(), is("DE000C21EMZ1"));
        assertThat(security.getName(), is("ODX5 C11500.00 29MAR19"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }
    
    private Security assertSecurityTransaktionsuebersicht2Second(Security security)
    {
        assertThat(security.getIsin(), is("DE000C25KFE8"));
        assertThat(security.getName(), is("ODX2 C11000.00 08FEB19"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }
    
    private Security assertSecurityTransaktionsuebersicht2Third(Security security)
    {
        assertThat(security.getIsin(), is("DE000C25KFF5"));
        assertThat(security.getName(), is("ODX2 P11000.00 08FEB19"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    private Security assertSecurityTransaktionsuebersicht2Fourth(Security security)
    {
        assertThat(security.getIsin(), is("DE000C25KFN9"));
        assertThat(security.getName(), is("ODX2 C11200.00 08FEB19"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }
    
    private Security assertSecurityTransaktionsuebersicht3(Security security)
    {
        assertThat(security.getIsin(), is("DE0005140008"));
        assertThat(security.getName(), is("DEUTSCHE BANK AG NA O.N"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }
    
    private Security assertSecurityTransaktionsuebersicht4(Security security)
    {
        assertThat(security.getIsin(), is("US88160R1014"));
        assertThat(security.getName(), is("TESLA MOTORS INC. - C"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        return security;
    }
    

    @Test
    public void testKontoauszug1()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DegiroKontoauszug1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        AccountTransaction transaction = (AccountTransaction) item.orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-08-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 350_00L)));

    }
    
    @Test
    public void testKontoauszug2()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DegiroKontoauszug2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-01T11:44")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 0_01L)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(4).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-03-05T15:37")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 18_00L)));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-07T11:53")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1000_00L)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-01T11:44")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 00_01L)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(2).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-03-01T13:21")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1_00L)));

    }
    
    @Test
    public void testKontoauszug3()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DegiroKontoauszug3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
        
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(5).getSubject();
        assertSecurityKontoauszug3First(transaction.getSecurity());
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 2_07L)));
        Unit taxUnit = transaction.getUnit(Unit.Type.TAX).orElseThrow(IllegalArgumentException::new);
        assertThat(taxUnit.getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.55))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(6).getSubject();
        assertSecurityKontoauszug3First(transaction.getSecurity());
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 22_64L)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(7).getSubject();
        assertSecurityKontoauszug3Second(transaction.getSecurity());
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 9L)));
        taxUnit = transaction.getUnit(Unit.Type.TAX).orElseThrow(IllegalArgumentException::new);
        assertThat(taxUnit.getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.02))));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(8).getSubject();
        assertSecurityKontoauszug3Second(transaction.getSecurity());
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1_74)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(9).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 7L)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(10).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 2_50L)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(11).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 75L)));
        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.86))));
        

    }
    
    @Test
    public void testKontoauszug4()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DegiroKontoauszug4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
        
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-08-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 89L)));

    }
    
    @Test
    public void testKontoauszug5()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DegiroKontoauszug5.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(38));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
        
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-06T15:34")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 200_00L)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(2).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-22T18:40")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 27_00L)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(9).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-22T18:40")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1_00L)));        
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(14).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-03T11:47")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 54L)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(15).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-03T11:47")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 54L)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(27).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-14T07:55")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 30L)));
        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.40))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(28).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-06T09:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 23L)));
        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.37))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.04))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(29).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-16T08:21")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 58L)));
        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.77))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.11))));
        
    }
    
    @Test
    public void testKontoauszug6()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DegiroKontoauszug6.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
        
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-25T13:06")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1000_00L)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-05T00:09")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1000_00L)));
        
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(2).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-26T13:52")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 2_00L)));
    }
        
        
    @Test
    public void testTransaktionsuebersicht2()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DegiroTransaktionsuebersicht2.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(49));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
        
        //29-03-2019 12:18 29-03-2019 ODX5 C11500.00 29MAR19 DE000C21EMZ1 Verkauf 3 zu je 30 EUR (DE000C21EMZ1) EUR 450,00 EUR 2.604,06
        // check first buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(0).getSubject();
        assertSecurityTransaktionsuebersicht2First(entry.getPortfolioTransaction().getSecurity());
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(447.30))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-03-29T12:18")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 2_70L)));
        
        // check xx buy sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(27).getSubject();
        assertSecurityTransaktionsuebersicht2Second(entry.getPortfolioTransaction().getSecurity());
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.90))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-02-08T12:44")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 90L)));
        
        // check xx buy sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(29).getSubject();
        assertSecurityTransaktionsuebersicht2Third(entry.getPortfolioTransaction().getSecurity());
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(132.80))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-02-08T10:54")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 180L)));
        
        // check xx buy sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(30).getSubject();
        assertSecurityTransaktionsuebersicht2Fourth(entry.getPortfolioTransaction().getSecurity());
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.90))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-02-07T13:02")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 90L)));

    }
    
    @Test
    public void testTransaktionsuebersicht3()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DegiroTransaktionsuebersicht3.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
        
        // check first buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(0).getSubject();
        assertSecurityTransaktionsuebersicht3(entry.getPortfolioTransaction().getSecurity());
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1010.94))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-01T15:35")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(136)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 2_26L)));
        
        // check xx buy sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(1).getSubject();
        assertSecurityTransaktionsuebersicht3(entry.getPortfolioTransaction().getSecurity());
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(132.38))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-01T12:20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(18)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 3L)));
        
        // check xx buy sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(2).getSubject();
        assertSecurityTransaktionsuebersicht3(entry.getPortfolioTransaction().getSecurity());
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(869.88))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-01T12:20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(118)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 223L)));

    }

    @Test
    public void testTransaktionsuebersicht4()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DegiroTransaktionsuebersicht4.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
        
        // check  sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(0).getSubject();
        assertSecurityTransaktionsuebersicht4(entry.getPortfolioTransaction().getSecurity());
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(644.53))));
        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(720.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-29T16:11")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 51L)));
        
        // check second buy transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(2).getSubject();
        assertSecurityTransaktionsuebersicht4(entry.getPortfolioTransaction().getSecurity());
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(210.42))));
        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(234.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-26T20:23")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 50L)));
        
        // check first buy  transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(3).getSubject();
        assertSecurityTransaktionsuebersicht4(entry.getPortfolioTransaction().getSecurity());
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(431.20))));
        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(480.00))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-26T17:52")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 51L)));

    }

    @Test
    public void testTransaktionsuebersicht5()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "DegiroTransaktionsuebersicht5.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("ARCHER-DANIELS-MIDLAND"));
        assertThat(security.getIsin(), is("US0394831020"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check first transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(75.91))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(84.52))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-25T19:03")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 1L)));

        // check second transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1822.39))));

        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2028.48))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-25T19:03")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(48)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 67L)));
    }

    @Test
    public void testTransacties1()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DegiroTransacties1.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security #1
        Security security = results.stream().filter(i -> i instanceof SecurityItem)
                        .filter(i -> ((SecurityItem) i).getSecurity().getIsin().equals("US5801351017")).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("MCDONALD'S CORPORATION"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check transaction #1
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .filter(i -> i.getSecurity() == security).findAny().orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(757.83))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(847.96))));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-09T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.51))));

        // check security #2
        Security security2 = results.stream().filter(i -> i instanceof SecurityItem)
                        .filter(i -> ((SecurityItem) i).getSecurity().getIsin().equals("IE00B3RBWM25")).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("VANGUARD FTSE AW"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction #2
        BuySellEntry entry2 = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .filter(i -> i.getSecurity() == security2).findAny().orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(entry2.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry2.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(231.30))));

        assertThat(entry2.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-09T14:08")));
        assertThat(entry2.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));
        assertThat(entry2.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0))));

        // check security #3
        Security security3 = results.stream().filter(i -> i instanceof SecurityItem)
                        .filter(i -> ((SecurityItem) i).getSecurity().getIsin().equals("US46284V1017")).findAny()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getName(), is("IRON MOUNTAIN INCORPOR"));
        assertThat(security3.getCurrencyCode(), is("USD"));

        // check transaction #3
        BuySellEntry entry3 = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .filter(i -> i.getSecurity() == security3).findAny().orElseThrow(IllegalArgumentException::new)
                        .getSubject();

        assertThat(entry3.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry3.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(564.36))));

        Unit grossValueUnit3 = entry3.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit3.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(632))));
        assertThat(entry3.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-05T20:52")));
        assertThat(entry3.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry3.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.57))));
    }
}
