package name.abuchen.portfolio.datatransfer.pdf.c24bankgmbh;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.C24BankGmbHPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class C24BankGmbHPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        C24BankGmbHPDFExtractor extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-17"), hasAmount("EUR", 1508.42), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-17"), hasAmount("EUR", 1115.22), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testKontoauszug02()
    {
        C24BankGmbHPDFExtractor extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2024-05-31"), hasAmount("EUR", 2.29), //
                        hasSource("Kontoauszug02.txt"), hasNote("Steuern"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-05-31"), hasAmount("EUR", 1.93), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zinsen"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-17"), hasAmount("EUR", 1460.11), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testKontoauszug03()
    {
        C24BankGmbHPDFExtractor extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2024-06-30"), hasAmount("EUR", 4.04), //
                        hasSource("Kontoauszug03.txt"), hasNote("Steuern"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-06-30"), hasAmount("EUR", 15.32), //
                        hasSource("Kontoauszug03.txt"), hasNote("Zinsen"))));
    }

    @Test
    public void testKontoauszug04()
    {
        C24BankGmbHPDFExtractor extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2024-07-31"), hasAmount("EUR", 4.18), //
                        hasSource("Kontoauszug04.txt"), hasNote("Steuern"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-07-31"), hasAmount("EUR", 15.86), //
                        hasSource("Kontoauszug04.txt"), hasNote("Zinsen"))));
    }

    @Test
    public void testKontoauszug05()
    {
        C24BankGmbHPDFExtractor extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-08-05"), hasAmount("EUR", 2800.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Echtzeitüberweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-08-05"), hasAmount("EUR", 2800.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisung"))));
    }
}
