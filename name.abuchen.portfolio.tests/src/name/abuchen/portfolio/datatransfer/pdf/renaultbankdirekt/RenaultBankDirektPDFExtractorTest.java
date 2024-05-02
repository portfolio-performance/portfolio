package name.abuchen.portfolio.datatransfer.pdf.renaultbankdirekt;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.RenaultBankDirektPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class RenaultBankDirektPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2019-11-18"), hasAmount("EUR", 4480.00), //
                        hasSource("Kontoauszug01.txt"))));

        assertThat(results, hasItem(deposit(hasDate("2019-11-18"), hasAmount("EUR", 351.50), //
                        hasSource("Kontoauszug01.txt"))));

        assertThat(results, hasItem(deposit(hasDate("2019-11-18"), hasAmount("EUR", 10000.00), //
                        hasSource("Kontoauszug01.txt"))));

        assertThat(results, hasItem(removal(hasDate("2019-11-21"), hasAmount("EUR", 210.00), //
                        hasSource("Kontoauszug01.txt"))));

        assertThat(results, hasItem(removal(hasDate("2019-11-29"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoauszug01.txt"))));

        assertThat(results, hasItem(interest(hasDate("2019-11-29"), hasAmount("EUR", 2.44), //
                        hasSource("Kontoauszug01.txt"))));
    }

    @Test
    public void testKontoauszug02()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2020-02-17"), hasAmount("EUR", 210), //
                        hasSource("Kontoauszug02.txt"))));

        assertThat(results, hasItem(interest(hasDate("2020-02-28"), hasAmount("EUR", 3.41), //
                        hasSource("Kontoauszug02.txt"))));

    }

    @Test
    public void testKontoauszug03()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(interest(hasDate("2020-06-30"), hasAmount("EUR", 0.21), //
                        hasSource("Kontoauszug03.txt"))));
    }

    @Test
    public void testKontoauszug04()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2021-02-17"), hasAmount("EUR", 150), //
                        hasSource("Kontoauszug04.txt"))));

        assertThat(results, hasItem(deposit(hasDate("2021-02-22"), hasAmount("EUR", 7547.85), //
                        hasSource("Kontoauszug04.txt"))));

        assertThat(results, hasItem(interest(hasDate("2021-02-26"), hasAmount("EUR", 0.52), //
                        hasSource("Kontoauszug04.txt"))));
    }

    @Test
    public void testKontoauszug05()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2021-04-19"), hasAmount("EUR", 10), //
                        hasSource("Kontoauszug05.txt"))));

        assertThat(results, hasItem(removal(hasDate("2021-04-19"), hasAmount("EUR", 10), //
                        hasSource("Kontoauszug05.txt"))));

        assertThat(results, hasItem(interest(hasDate("2021-04-30"), hasAmount("EUR", 0.73), //
                        hasSource("Kontoauszug05.txt"))));
    }

    @Test
    public void testKontoauszug06()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2021-12-04"), hasAmount("EUR", 3200), //
                        hasSource("Kontoauszug06.txt"))));

        assertThat(results, hasItem(removal(hasDate("2021-12-22"), hasAmount("EUR", 5000), //
                        hasSource("Kontoauszug06.txt"))));

        assertThat(results, hasItem(interest(hasDate("2021-12-31"), hasAmount("EUR", 1.23), //
                        hasSource("Kontoauszug06.txt"))));

        assertThat(results, hasItem(interest(hasDate("2021-12-31"), hasAmount("EUR", 2.46), //
                        hasSource("Kontoauszug06.txt"))));

        assertThat(results, hasItem(taxes(hasDate("2021-12-31"), hasAmount("EUR", 0.62), //
                        hasSource("Kontoauszug06.txt"))));
    }

    @Test
    public void testKontoauszug07()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2020-11-13"), hasAmount("EUR", 616), //
                        hasSource("Kontoauszug07.txt"))));

        assertThat(results, hasItem(deposit(hasDate("2020-11-16"), hasAmount("EUR", 7000), //
                        hasSource("Kontoauszug07.txt"))));

        assertThat(results, hasItem(deposit(hasDate("2020-11-20"), hasAmount("EUR", 5400), //
                        hasSource("Kontoauszug07.txt"))));

        assertThat(results, hasItem(removal(hasDate("2020-11-03"), hasAmount("EUR", 2300), //
                        hasSource("Kontoauszug07.txt"))));

        assertThat(results, hasItem(removal(hasDate("2020-11-04"), hasAmount("EUR", 2200), //
                        hasSource("Kontoauszug07.txt"))));

        assertThat(results, hasItem(removal(hasDate("2020-11-13"), hasAmount("EUR", 400), //
                        hasSource("Kontoauszug07.txt"))));

        assertThat(results, hasItem(removal(hasDate("2020-11-17"), hasAmount("EUR", 5800), //
                        hasSource("Kontoauszug07.txt"))));
    }

    @Test
    public void testKontoauszug08()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

        assertThat(errors, empty());

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2022-06-21"), hasAmount("EUR", 5000), //
                        hasSource("Kontoauszug08.txt"))));

        assertThat(results, hasItem(removal(hasDate("2022-06-10"), hasAmount("EUR", 1000), //
                        hasSource("Kontoauszug08.txt"))));

        assertThat(results, hasItem(removal(hasDate("2022-06-29"), hasAmount("EUR", 2500), //
                        hasSource("Kontoauszug08.txt"))));

        assertThat(results, hasItem(removal(hasDate("2022-06-30"), hasAmount("EUR", 2000), //
                        hasSource("Kontoauszug08.txt"))));

        assertThat(results, hasItem(interest(hasDate("2022-06-30"), hasAmount("EUR", 0.60), //
                        hasSource("Kontoauszug08.txt"))));
    }

    @Test
    public void testKontoauszug09()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2020-08-13"), hasAmount("EUR", 616), //
                        hasSource("Kontoauszug09.txt"))));

        assertThat(results, hasItem(removal(hasDate("2020-08-04"), hasAmount("EUR", 500), //
                        hasSource("Kontoauszug09.txt"))));

        assertThat(results, hasItem(removal(hasDate("2020-08-07"), hasAmount("EUR", 250), //
                        hasSource("Kontoauszug09.txt"))));

        assertThat(results, hasItem(removal(hasDate("2020-08-10"), hasAmount("EUR", 400), //
                        hasSource("Kontoauszug09.txt"))));

        assertThat(results, hasItem(removal(hasDate("2020-08-31"), hasAmount("EUR", 1500), //
                        hasSource("Kontoauszug09.txt"))));

        assertThat(results, hasItem(interest(hasDate("2020-08-31"), hasAmount("EUR", 5.82), //
                        hasSource("Kontoauszug09.txt"))));
    }

    @Test
    public void testKontoauszug10()
    {
        RenaultBankDirektPDFExtractor extractor = new RenaultBankDirektPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2022-08-12"), hasAmount("EUR", 4250), //
                        hasSource("Kontoauszug10.txt"))));

        assertThat(results, hasItem(removal(hasDate("2022-08-01"), hasAmount("EUR", 1500), //
                        hasSource("Kontoauszug10.txt"))));

        assertThat(results, hasItem(removal(hasDate("2022-08-05"), hasAmount("EUR", 600), //
                        hasSource("Kontoauszug10.txt"))));

        assertThat(results, hasItem(removal(hasDate("2022-08-23"), hasAmount("EUR", 1500), //
                        hasSource("Kontoauszug10.txt"))));

        assertThat(results, hasItem(removal(hasDate("2022-08-30"), hasAmount("EUR", 3500), //
                        hasSource("Kontoauszug10.txt"))));

        assertThat(results, hasItem(interest(hasDate("2022-08-05"), hasAmount("EUR", 1.62), //
                        hasSource("Kontoauszug10.txt"))));

        assertThat(results, hasItem(interest(hasDate("2022-08-31"), hasAmount("EUR", 3.82), //
                        hasSource("Kontoauszug10.txt"))));

        assertThat(results, hasItem(interestCharge(hasDate("2022-08-05"), hasAmount("EUR", 3.59), //
                        hasSource("Kontoauszug10.txt"))));

    }
}
