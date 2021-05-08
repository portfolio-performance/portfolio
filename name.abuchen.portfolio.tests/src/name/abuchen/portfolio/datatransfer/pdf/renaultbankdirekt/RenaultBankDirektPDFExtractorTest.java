package name.abuchen.portfolio.datatransfer.pdf.renaultbankdirekt;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.InputFile;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.RenaultBankDirektPDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

public class RenaultBankDirektPDFExtractorTest
{
    @Test
    public void testKontoauszug1()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("renaultBankDirektKontoauszug1.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DEPOSIT)
                        .count(), is(3L));
        
        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(1L));
        
        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.REMOVAL)
                        .count(), is(2L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-11-18T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4480)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-11-18T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(351.50)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-11-18T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10000)));

        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-11-21T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(210)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-11-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(300)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-11-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.44)));

    }

    @Test
    public void testKontoauszug2()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("renaultBankDirektKontoauszug2.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DEPOSIT)
                        .count(), is(1L));

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(1L));
        
        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.REMOVAL)
                        .count(), is(0L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-02-17T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(210)));

        item = iter.next();

        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-02-28T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(3.41)));

    }
    
    @Test
    public void testKontoauszug3()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("renaultBankDirektKontoauszug3.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DEPOSIT)
                        .count(), is(0L));

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(1L));
        
        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.REMOVAL)
                        .count(), is(0L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.21)));
    }
    
    @Test
    public void testKontoauszug4()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("renaultBankDirektKontoauszug4.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DEPOSIT)
                        .count(), is(2L));

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(1L));
        
        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.REMOVAL)
                        .count(), is(0L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-17T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(150)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-22T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(7547.85)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-26T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.52)));
    }

    @Test
    public void testKontoauszug5()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("renaultBankDirektKontoauszug5.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DEPOSIT)
                        .count(), is(1L));

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(1L));
        
        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.REMOVAL)
                        .count(), is(1L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-19T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-19T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.73)));
    }
    
    private List<InputFile> loadFile(String filename)
    {
        return PDFInputFile.loadTestCase(getClass(), filename);
    }
}
