package name.abuchen.portfolio.datatransfer.pdf.bondora;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.BondoraGoAndGrowPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class BondoraGoAndGrowPDFExtractorTest
{

    @Test
    public void testImportKontoauszug() throws IOException
    {
        BondoraGoAndGrowPDFExtractor extractor = new BondoraGoAndGrowPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KontoauszugGoAndGrow.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(211));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check deposit
        Optional<Item> deposit = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(deposit.isPresent(), is(true));
        assertThat(deposit.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction depositTransaction = (AccountTransaction) deposit.get().getSubject();
        assertThat(depositTransaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(depositTransaction.getDateTime(), is(LocalDateTime.parse("2020-01-15T00:00")));
        assertThat(depositTransaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 100_00L)));

        // check removal
        Optional<Item> removal = results.stream().filter(i -> i instanceof TransactionItem).skip(results.size() - 1)
                        .findFirst();
        assertThat(removal.isPresent(), is(true));
        assertThat(removal.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction removalTransaction = (AccountTransaction) removal.get().getSubject();
        assertThat(removalTransaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(removalTransaction.getDateTime(), is(LocalDateTime.parse("2020-08-07T00:00")));
        assertThat(removalTransaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 203_00L)));

        // check interest
        Optional<Item> interest = results.stream().filter(i -> i instanceof TransactionItem).skip(results.size() - 2)
                        .findFirst();
        assertThat(interest.isPresent(), is(true));
        assertThat(interest.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction interestTransaction = (AccountTransaction) interest.get().getSubject();
        assertThat(interestTransaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(interestTransaction.getDateTime(), is(LocalDateTime.parse("2020-08-06T00:00")));
    }

    @Test
    public void testImportKontoauszug02() throws IOException
    {
        BondoraGoAndGrowPDFExtractor extractor = new BondoraGoAndGrowPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KontoauszugGoAndGrow02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check deposit
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction depositTransaction = (AccountTransaction) item.get().getSubject();
        assertThat(depositTransaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(depositTransaction.getDateTime(), is(LocalDateTime.parse("2020-09-04T00:00")));
        assertThat(depositTransaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 200_00L)));

        // check interest
        item = results.stream().filter(i -> i instanceof TransactionItem).skip(1).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction interestTransaction = (AccountTransaction) item.get().getSubject();
        assertThat(interestTransaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(interestTransaction.getDateTime(), is(LocalDateTime.parse("2020-09-05T00:00")));
        assertThat(interestTransaction.getMonetaryAmount(), is(Money.of("EUR", 0_04)));

        // check interest
        item = results.stream().filter(i -> i instanceof TransactionItem).skip(2).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        interestTransaction = (AccountTransaction) item.get().getSubject();
        assertThat(interestTransaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(interestTransaction.getDateTime(), is(LocalDateTime.parse("2020-09-06T00:00")));
        assertThat(interestTransaction.getMonetaryAmount(), is(Money.of("EUR", 0_03)));

    }

    @Test
    public void testImportKontoauszug03() throws IOException
    {
        BondoraGoAndGrowPDFExtractor extractor = new BondoraGoAndGrowPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<Exception>();
    
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KontoauszugGoAndGrow03.txt"),
                        errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        // check interest
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1_00L)));
    
        // check interest
        item = results.stream().filter(i -> i instanceof TransactionItem).skip(1).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-26T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1_01L)));
    
        // check deposit
        item = results.stream().filter(i -> i instanceof TransactionItem).skip(2).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 300_00L)));
    
    }

    @Test
    public void testImportKontoauszug04() throws IOException
    {
        BondoraGoAndGrowPDFExtractor extractor = new BondoraGoAndGrowPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<Exception>();
    
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KontoauszugGoAndGrow04.txt"),
                        errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        // check interest
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-18T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.31))));
    
        // check interest
        item = results.stream().filter(i -> i instanceof TransactionItem).skip(1).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-19T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.3))));
    
        // check removal
        item = results.stream().filter(i -> i instanceof TransactionItem).skip(2).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-20T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
    
    }

    @Test
    public void testImportKontoauszug05() throws IOException
    {
        BondoraGoAndGrowPDFExtractor extractor = new BondoraGoAndGrowPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<Exception>();
    
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KontoauszugGoAndGrow05.txt"),
                        errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        // check interest
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-07T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500))));
    
        // check interest
        item = results.stream().filter(i -> i instanceof TransactionItem).skip(1).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.27))));
    }

    @Test
    public void testImportKontoauszug06() throws IOException
    {
        BondoraGoAndGrowPDFExtractor extractor = new BondoraGoAndGrowPDFExtractor(new Client());
    
        List<Exception> errors = new ArrayList<Exception>();
    
        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KontoauszugGoAndGrow06.txt"),
                        errors);
    
        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);
    
        // check deposit #1
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25))));

        // check deposit #2
        item = results.stream().filter(i -> i instanceof TransactionItem).skip(1).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.91))));
    
        // check interest #1
        item = results.stream().filter(i -> i instanceof TransactionItem).skip(2).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.02))));
    
        // check interest #1
        item = results.stream().filter(i -> i instanceof TransactionItem).skip(3).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
    }

}
