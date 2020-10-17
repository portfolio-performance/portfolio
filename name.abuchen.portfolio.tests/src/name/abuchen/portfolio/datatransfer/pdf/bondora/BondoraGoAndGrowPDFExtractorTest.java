package name.abuchen.portfolio.datatransfer.pdf.bondora;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

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

}
