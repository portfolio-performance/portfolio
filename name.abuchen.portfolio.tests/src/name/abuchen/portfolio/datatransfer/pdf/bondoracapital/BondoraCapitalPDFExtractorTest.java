package name.abuchen.portfolio.datatransfer.pdf.bondoracapital;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.BondoraCapitalPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
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

        // check deposit
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-15T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Überweisen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.00))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(209)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-06T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.31))));

        // check removal
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(210)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-07T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Abheben"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(203.00))));
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

        // check deposit
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-04T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Überweisen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.00))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-05T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.04))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-06T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.03))));
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

        // check interest
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-25T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-26T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.01))));

        // check deposit
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-02T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Überweisen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(300.00))));

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

        // check interest
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-18T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Go & Grow returns"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.31))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-19T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Go & Grow returns"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.30))));

        // check removal
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-20T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Withdrawal"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

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

        // check interest
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-07T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-08T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.27))));
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

        // check deposit
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-05T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Überweisen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25))));

        // check deposit
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-06T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Überweisen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.91))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-06T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.02))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-31T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
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

        // check deposit
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-25T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug07.txt"));
        assertThat(transaction.getNote(), is("Überweisen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.00))));

        // check deposit
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-27T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug07.txt"));
        assertThat(transaction.getNote(), is("Überweisen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.00))));

        // check deposit
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-27T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug07.txt"));
        assertThat(transaction.getNote(), is("Überweisen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(980.00))));
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

        // check interest
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-01-29T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.22))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-01-30T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.22))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-01-31T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.22))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-01T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.22))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(4)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-02T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.22))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(5)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-03T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.22))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(6)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-04T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.22))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(7)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-05T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.22))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(8)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-06T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.22))));

        // check deposit
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(9)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-07T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Überweisen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(10)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-07T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.22))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(11)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-08T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.40))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(12)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-09T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.39))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(13)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-10T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.40))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(14)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-11T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.40))));

        // check interest
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(15)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-12T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Go & Grow Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.40))));
    }
}
