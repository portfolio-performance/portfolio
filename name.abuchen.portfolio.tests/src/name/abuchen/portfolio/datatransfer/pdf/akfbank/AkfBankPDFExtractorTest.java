package name.abuchen.portfolio.datatransfer.pdf.akfbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
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
import name.abuchen.portfolio.datatransfer.pdf.AkfBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class AkfBankPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-04-08"), hasAmount("EUR", 29000.12), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-04-21"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-04-29"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2011-04-30"), hasAmount("EUR", 44.20), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug02()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2012-12-03"), hasAmount("EUR", 150.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2012-12-05"), hasAmount("EUR", 1660.89), //
                        hasSource("Kontoauszug02.txt"), hasNote("DTA Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2012-12-31"), hasAmount("EUR", 0.09), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2012-12-31"), hasAmount("EUR", 0.33), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug03()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-13"), hasAmount("EUR", 1.24), //
                        hasSource("Kontoauszug03.txt"), hasNote("SEPA Überweisung online"))));
    }

    @Test
    public void testKontoauszug04()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-07-31"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug05()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-31"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug06()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-08-16"), hasAmount("EUR", 755.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("SEPA Gutschrift Bank"))));
    }

    @Test
    public void testKontoauszug07()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2012-11-14"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2012-11-30"), hasAmount("EUR", 2.71), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug08()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2021-09-12"), hasAmount("EUR", 6.66), //
                        hasSource("Kontoauszug08.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-09-12"), hasAmount("EUR", 4446.66), //
                        hasSource("Kontoauszug08.txt"), hasNote("Festgeld Anlage"))));
    }

    @Test
    public void testKontoauszug09()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-11-01"), hasAmount("EUR", 5742.27), //
                        hasSource("Kontoauszug09.txt"), hasNote("Sparkonto Kündigung"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2022-11-30"), hasAmount("EUR", 0.05), //
                        hasSource("Kontoauszug09.txt"), hasNote(null))));
    }
}
