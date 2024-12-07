package name.abuchen.portfolio.datatransfer.pdf.estateguru;

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
import name.abuchen.portfolio.datatransfer.pdf.EstateGuruPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class EstateGuruPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        EstateGuruPDFExtractor extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-07-25"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-15"), hasAmount("EUR", 1100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-02-02"), hasAmount("EUR", 51.70), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug02()
    {
        EstateGuruPDFExtractor extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-06-21"), hasAmount("EUR", 0.42), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zins"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-05-02"), hasAmount("EUR", 0.10), //
                        hasSource("Kontoauszug02.txt"), hasNote("Strafe"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-04-10"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug02.txt"), hasNote("Entschädigung"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-04-05"), hasAmount("EUR", 0.61), //
                        hasSource("Kontoauszug02.txt"), hasNote("Vermögensverwaltungsgebühr"))));
    }

    @Test
    public void testKontoauszug03()
    {
        EstateGuruPDFExtractor extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-09-28"), hasAmount("EUR", 0.42), //
                        hasSource("Kontoauszug03.txt"), hasNote("Interest"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-09-27"), hasAmount("EUR", 1.25), //
                        hasSource("Kontoauszug03.txt"), hasNote("Interest"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-11"), hasAmount("EUR", 5.41), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-09-06"), hasAmount("EUR", 1.12), //
                        hasSource("Kontoauszug03.txt"), hasNote("Interest"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-09-05"), hasAmount("EUR", 0.24), //
                        hasSource("Kontoauszug03.txt"), hasNote("AUM"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-08-30"), hasAmount("EUR", 0.12), //
                        hasSource("Kontoauszug03.txt"), hasNote("Interest"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-08-28"), hasAmount("EUR", 0.46), //
                        hasSource("Kontoauszug03.txt"), hasNote("Interest"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-22"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug04()
    {
        EstateGuruPDFExtractor extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(18L));
        assertThat(results.size(), is(18));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-28"), hasAmount("EUR", 0.46), //
                        hasSource("Kontoauszug04.txt"), hasNote("Interest"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-15"), hasAmount("EUR", 5.67), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-20"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-20"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-06"), hasAmount("EUR", 0.13), //
                        hasSource("Kontoauszug04.txt"), hasNote("Interest"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-06"), hasAmount("EUR", 5.82), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-04"), hasAmount("EUR", 273.30), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-12-04"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Withdraw fee"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-12-02"), hasAmount("EUR", 0.41), //
                        hasSource("Kontoauszug04.txt"), hasNote("Sale fee"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-12-02"), hasAmount("EUR", 0.41), //
                        hasSource("Kontoauszug04.txt"), hasNote("Sale fee"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-02"), hasAmount("EUR", 13.54), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-11-01"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug04.txt"), hasNote("Indemnity"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-03"), hasAmount("EUR", 30.00), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-09-20"), hasAmount("EUR", 0.27), //
                        hasSource("Kontoauszug04.txt"), hasNote("Secondary Market Profit"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-09-20"), hasAmount("EUR", 0.95), //
                        hasSource("Kontoauszug04.txt"), hasNote("Sale fee"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-09-20"), hasAmount("EUR", 31.28), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-02-10"), hasAmount("EUR", 0.36), //
                        hasSource("Kontoauszug04.txt"), hasNote("Penalty"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-01-03"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug04.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2022-12-15"), hasAmount("EUR", 1.25), //
                        hasSource("Kontoauszug04.txt"), hasNote("Interest"))));
    }

    @Test
    public void testKontoauszug05()
    {
        EstateGuruPDFExtractor extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-10-05"), hasAmount("EUR", 0.24), //
                        hasSource("Kontoauszug05.txt"), hasNote("Vermögensverwaltungsgebühr"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-10-01"), hasAmount("EUR", 0.59), //
                        hasSource("Kontoauszug05.txt"), hasNote("Zins"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-10-01"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug06()
    {
        EstateGuruPDFExtractor extractor = new EstateGuruPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
//        assertThat(countAccountTransactions(results), is(3L));
//        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-28"), hasAmount("EUR", 0.46), //
                        hasSource("Kontoauszug06.txt"), hasNote("Zins"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-20"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-15"), hasAmount("EUR", 5.67), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-08"), hasAmount("EUR", 0.48), //
                        hasSource("Kontoauszug06.txt"), hasNote("Zins"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-06"), hasAmount("EUR", 0.13), //
                        hasSource("Kontoauszug06.txt"), hasNote("Zins"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-04"), hasAmount("EUR", 273.30), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-12-04"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Abhebegebühr"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-12-02"), hasAmount("EUR", 0.41), //
                        hasSource("Kontoauszug06.txt"), hasNote("Verkaufsgebühr"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-02"), hasAmount("EUR", 13.54), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-11-01"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug06.txt"), hasNote("Entschädigung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-03"), hasAmount("EUR", 30.00), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-09-20"), hasAmount("EUR", 0.27), //
                        hasSource("Kontoauszug06.txt"), hasNote("Gewinn auf dem Zweitmarkt"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-09-20"), hasAmount("EUR", 0.95), //
                        hasSource("Kontoauszug06.txt"), hasNote("Verkaufsgebühr"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-09-20"), hasAmount("EUR", 31.28), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2022-11-29"), hasAmount("EUR", 0.22), //
                        hasSource("Kontoauszug06.txt"), hasNote("Strafe"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-01-03"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2022-12-15"), hasAmount("EUR", 1.25), //
                        hasSource("Kontoauszug06.txt"), hasNote("Zins"))));
    }
}
