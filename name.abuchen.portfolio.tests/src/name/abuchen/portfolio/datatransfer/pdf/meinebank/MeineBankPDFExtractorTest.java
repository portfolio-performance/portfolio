package name.abuchen.portfolio.datatransfer.pdf.meinebank;

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
import name.abuchen.portfolio.datatransfer.pdf.MeineBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class MeineBankPDFExtractorTest
{
    @Test
    public void testKontoauszug1()
    {
        MeineBankPDFExtractor extractor = new MeineBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("meinebankKontoauszug1.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DEPOSIT)
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
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-06-20T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(26350)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-06-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(500)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-06-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1700)));
    }
    
    @Test
    public void testKontoauszug2()
    {
        MeineBankPDFExtractor extractor = new MeineBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("meinebankKontoauszug2.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(1L));

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.REMOVAL)
                        .count(), is(6L));
        
        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.TAXES)
                        .count(), is(3L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100)));    
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-06T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(400)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-11T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(900)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-13T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(700)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-14T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(5400)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10000)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(534.59)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.46)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.76)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(8.46)));
    }
    

    private List<InputFile> loadFile(String filename)
    {
        return PDFInputFile.loadTestCase(getClass(), filename);
    }
}
