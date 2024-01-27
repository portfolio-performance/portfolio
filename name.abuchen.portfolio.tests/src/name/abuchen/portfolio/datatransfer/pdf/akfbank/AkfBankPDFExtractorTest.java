package name.abuchen.portfolio.datatransfer.pdf.akfbank;

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
import name.abuchen.portfolio.datatransfer.pdf.AkfBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class AkfBankPDFExtractorTest
{
    @Test
    public void testTagesgeldKontoauzug01()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("TagesgeldKontoauzug01.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DEPOSIT)
                        .count(), is(3L));

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(1L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2011-04-08T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(29000.12)));
        
        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2011-04-21T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2500)));

        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2011-04-29T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2500)));

        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2011-04-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(44.20)));
    }
    
    @Test
    public void testTagesgeldKontoauzug02()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("TagesgeldKontoauzug02.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DEPOSIT)
                        .count(), is(1L));

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(2L));

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.REMOVAL)
                        .count(), is(1L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2012-12-03T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(150)));

        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2012-12-05T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1660.89)));

        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2012-12-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.09)));

        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2012-12-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.33)));
    }

    @Test
    public void testTagesgeldKontoauzug03()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("TagesgeldKontoauzug03.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.REMOVAL)
                        .count(), is(1L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-04-13T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.24)));
    }

    @Test
    public void testTagesgeldKontoauzug04()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("TagesgeldKontoauzug04.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(1L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-07-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.01)));
    }
    
    @Test
    public void testTagesgeldKontoauzug05()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("TagesgeldKontoauzug05.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(1L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.01)));
    }

    @Test
    public void testTagesgeldKontoauzug06()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("TagesgeldKontoauzug06.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.DEPOSIT)
                        .count(), is(1L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-08-16T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(755.00)));
    }

    @Test
    public void testTagesgeldKontoauzug07()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("TagesgeldKontoauzug07.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(1L));

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.FEES)
                        .count(), is(1L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2012-11-14T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(5.00)));

        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2012-11-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.71)));
    }

    @Test
    public void testFestgeldKontoauzug01()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("FestgeldKontoauzug01.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(1L));

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.REMOVAL)
                        .count(), is(1L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-12T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4446.66)));

        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-12T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(6.66)));
    }

    @Test
    public void testAnlagekontoKontoauzug01()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("AnlagekontoKontoauzug01.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject())
                                        .getType() == AccountTransaction.Type.INTEREST)
                        .count(), is(1L));

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.REMOVAL)
                        .count(), is(1L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(5742.27)));

        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.05)));
    }

    private List<InputFile> loadFile(String filename)
    {
        return PDFInputFile.loadTestCase(getClass(), filename);
    }
}
