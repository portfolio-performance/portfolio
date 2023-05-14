package name.abuchen.portfolio.datatransfer.pdf.starmoney;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.StarMoneyPDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class StarMoneyPDFExtractorTest
{
    @Test
    public void testGiroKontoauszug01()
    {
        StarMoneyPDFExtractor extractor = new StarMoneyPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-07-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(103.75))));
        assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-07-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));
    }

    @Test
    public void testGiroKontoauszug02()
    {
        StarMoneyPDFExtractor extractor = new StarMoneyPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(11));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(11L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-09-24T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(72.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-09-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(259.85))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-09-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.98))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-09-29T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(418.16))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-09-29T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(227.21))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-09-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-09-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1111.06))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lohn, Gehalt, Rente"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-10-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(39.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-10-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.28))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-10-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(356.25))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2008-09-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.15))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Abrechnungszeitraum 01.07.2008 bis 30.09.2008"));
    }

    @Test
    public void testGiroKontoauszug03()
    {
        StarMoneyPDFExtractor extractor = new StarMoneyPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-08-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(54.69))));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-09-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(29.50))));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Basis-Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-09-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-08-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.20))));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Entgelte 01.08.2015 bis 31.08.2015"));
    }

    @Test
    public void testGiroKontoauszug04()
    {
        StarMoneyPDFExtractor extractor = new StarMoneyPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-12-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(400.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-12-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(119.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Basis-Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-12-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(28.50))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Basis-Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-11-28T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.55 - 1.80))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Entgelte 01.11.2014 bis 28.11.2014"));
    }

    @Test
    public void testGiroKontoauszug05()
    {
        StarMoneyPDFExtractor extractor = new StarMoneyPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2012-11-26T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(440.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2012-11-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1111.11))));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Lohn, Gehalt, Rente"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2012-11-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.30 - 1.80))));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Entgelte 01.11.2012 bis 30.11.2012"));
    }

    @Test
    public void testGiroKontoauszug06()
    {
        StarMoneyPDFExtractor extractor = new StarMoneyPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2013-12-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(187.20))));
        assertThat(transaction.getSource(), is("GiroKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2013-12-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.74))));
        assertThat(transaction.getSource(), is("GiroKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Abrechnungszeitraum 01.10.2013 bis 31.12.2013"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2013-12-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.30))));
        assertThat(transaction.getSource(), is("GiroKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Entgelte 30.11.2013 bis 30.12.2013"));
    }

    @Test
    public void testGiroKontoauszug07()
    {
        StarMoneyPDFExtractor extractor = new StarMoneyPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2012-12-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(750.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug07.txt"));
        assertThat(transaction.getNote(), is("Geldautomat"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2012-12-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug07.txt"));
        assertThat(transaction.getNote(), is("Abrechnungszeitraum 01.10.2012 bis 31.12.2012"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2012-12-28T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.30 - 1.80))));
        assertThat(transaction.getSource(), is("GiroKontoauszug07.txt"));
        assertThat(transaction.getNote(), is("Entgelte 01.12.2012 bis 28.12.2012"));

        // check cancellation transaction (Amount = 0,00)
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((AccountTransaction) cancellation.getSubject()).getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorTransactionTypeNotSupported));

        assertThat(((Transaction) cancellation.getSubject()).getDateTime(), is(LocalDateTime.parse("2012-12-31T00:00")));
        assertThat(((Transaction) cancellation.getSubject()).getShares(), is(Values.Share.factorize(0)));
        assertThat(((Transaction) cancellation.getSubject()).getSource(), is("GiroKontoauszug07.txt"));
        assertThat(((Transaction) cancellation.getSubject()).getNote(), is("Abrechnungszeitraum 01.10.2012 bis 31.12.2012"));

        assertThat(((Transaction) cancellation.getSubject()).getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }
}
