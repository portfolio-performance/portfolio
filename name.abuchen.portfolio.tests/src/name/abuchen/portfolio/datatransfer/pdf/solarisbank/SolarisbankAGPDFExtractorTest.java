package name.abuchen.portfolio.datatransfer.pdf.solarisbank;

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
import name.abuchen.portfolio.datatransfer.pdf.SolarisbankAGPDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SolarisbankAGPDFExtractorTest
{
    @Test
    public void testGiroKontoauszug01()
    {
        SolarisbankAGPDFExtractor extractor = new SolarisbankAGPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("GiroKontoauszug01.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DEPOSIT)
                        .count(), is(2L));

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.REMOVAL)
                        .count(), is(2L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-26T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(200)));

        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-27T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(150)));

        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-26T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100)));

        item = iter.next();
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-28T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(16.10)));

    }

    @Test
    public void testGiroKontoauszug02()
    {
        SolarisbankAGPDFExtractor extractor = new SolarisbankAGPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("GiroKontoauszug02.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getType() == AccountTransaction.Type.DEPOSIT)
                        .count(), is(1L));

        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        Item item = iter.next();

        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-26T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(100)));
    }


    private List<InputFile> loadFile(String filename)
    {
        return PDFInputFile.loadTestCase(getClass(), filename);
    }
}
