package name.abuchen.portfolio.datatransfer.pdf.barclays;

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
import name.abuchen.portfolio.datatransfer.pdf.BarclaysPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class BarclaysPDFExtractorTest
{
    @Test
    public void testKreditKontoauszug01()
    {
        BarclaysPDFExtractor extractor = new BarclaysPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(7));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(7L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-11-20T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(119.96)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("GetYourGuide Tickets   Berlin"));
        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-11-20T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(21.84)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("ALDI ALBUFEIRA         ALBUFEIRA"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-11-20T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(8)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("GRUPO PESTANA          ALCACER DO SA"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-01T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(34.99)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("eBay O*00-00000-00000  Luxembourg"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-06T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *IONOS SE       00000000000"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-09T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(8.48)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Globus Baumarkt        Ort"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-09T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.29)));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Tegut Filiale 0000     LangOrtsnamen"));

    }
    
    @Test
    public void testKreditKontoauszug02()
    {
        BarclaysPDFExtractor extractor = new BarclaysPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(10));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(10L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-28T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(60.78)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lidl sagt Danke        Ort"));
        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2024-01-08T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(5)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *VODAFONE       0000000000"));
        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2024-01-06T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(671.99)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Per Lastschrift dankend erhalten"));
        item = iter.next();
        
        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2024-01-08T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Vorname Nachname"));
        item = iter.next();
        
        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2024-01-08T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(.5)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Gutschrift Manuelle Lastschrift"));
        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2024-01-04T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("TooGoodToG xxxxxxxxxxx toogoodtogo.d"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2024-01-05T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *IONOS SE       00000000000"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-25T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(60.78)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lidl sagt Danke        Ort"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-12-25T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(60.78)));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lidl sagt Danke        Ort"));
    }
}
