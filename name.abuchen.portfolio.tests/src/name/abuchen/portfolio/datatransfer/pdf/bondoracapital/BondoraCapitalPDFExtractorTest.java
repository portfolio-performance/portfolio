package name.abuchen.portfolio.datatransfer.pdf.bondoracapital;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.BondoraCapitalPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class BondoraCapitalPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        BondoraCapitalPDFExtractor extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(211));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(211L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-15T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(100.00)));
            assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Überweisen"));
        }

        iter = results.stream().filter(TransactionItem.class::isInstance).skip(1).iterator();
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-16T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.02)));
            assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        }

        iter = results.stream().filter(TransactionItem.class::isInstance).skip(210).iterator();
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-07T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(203.00)));
            assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
            assertThat(transaction.getNote(), is("Abheben"));
        }
    }

    @Test
    public void testKontoauszug02()
    {
        BondoraCapitalPDFExtractor extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-04T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(200.00)));
            assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
            assertThat(transaction.getNote(), is("Überweisen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-05T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.04)));
            assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
            assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-06T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.03)));
            assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
            assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-07T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.04)));
            assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
            assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        }
    }

    @Test
    public void testKontoauszug03()
    {
        BondoraCapitalPDFExtractor extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-25T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.00)));
            assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
            assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-26T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.01)));
            assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
            assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-02T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(300.00)));
            assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
            assertThat(transaction.getNote(), is("Überweisen"));
        }
    }

    @Test
    public void testKontoauszug04()
    {
        BondoraCapitalPDFExtractor extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-18T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.31)));
            assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
            assertThat(transaction.getNote(), is("Go & Grow returns"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-19T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.30)));
            assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
            assertThat(transaction.getNote(), is("Go & Grow returns"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-20T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.00)));
            assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
            assertThat(transaction.getNote(), is("Withdrawal"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-20T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.10)));
            assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
            assertThat(transaction.getNote(), is("Go & Grow returns"));
        }
    }

    @Test
    public void testKontoauszug05()
    {
        BondoraCapitalPDFExtractor extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-07T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1500.00)));
            assertThat(transaction.getSource(), is("Kontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Überweisen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-08T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.27)));
            assertThat(transaction.getSource(), is("Kontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-09T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.27)));
            assertThat(transaction.getSource(), is("Kontoauszug05.txt"));
            assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        }
    }

    @Test
    public void testKontoauszug06()
    {
        BondoraCapitalPDFExtractor extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-05T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(25.00)));
            assertThat(transaction.getSource(), is("Kontoauszug06.txt"));
            assertThat(transaction.getNote(), is("Überweisen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-06T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.91)));
            assertThat(transaction.getSource(), is("Kontoauszug06.txt"));
            assertThat(transaction.getNote(), is("Überweisen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-06T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.02)));
            assertThat(transaction.getSource(), is("Kontoauszug06.txt"));
            assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-31T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.06)));
            assertThat(transaction.getSource(), is("Kontoauszug06.txt"));
            assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        }
    }

    @Test
    public void testKontoauszug07()
    {
        BondoraCapitalPDFExtractor extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-25T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(5.00)));
            assertThat(transaction.getSource(), is("Kontoauszug07.txt"));
            assertThat(transaction.getNote(), is("Überweisen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-27T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(15.00)));
            assertThat(transaction.getSource(), is("Kontoauszug07.txt"));
            assertThat(transaction.getNote(), is("Überweisen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-27T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(980.00)));
            assertThat(transaction.getSource(), is("Kontoauszug07.txt"));
            assertThat(transaction.getNote(), is("Überweisen"));
        }
    }

    @Test
    public void testKontoauszug08()
    {
        BondoraCapitalPDFExtractor extractor = new BondoraCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(16));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(16L));
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-01-29T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.22)));
            assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
            assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        }

        iter = results.stream().filter(TransactionItem.class::isInstance).skip(9).iterator();
        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-07T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(1000.00)));
            assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
            assertThat(transaction.getNote(), is("Überweisen"));
        }

        if (iter.hasNext())
        {
            Item item = iter.next();

            // assert transaction
            AccountTransaction transaction = (AccountTransaction) item.getSubject();
            assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
            assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
            assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-07T00:00")));
            assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.22)));
            assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
            assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        }
    }
}
