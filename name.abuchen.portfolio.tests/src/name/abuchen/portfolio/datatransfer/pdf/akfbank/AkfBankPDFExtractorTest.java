package name.abuchen.portfolio.datatransfer.pdf.akfbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
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
                        hasSource("Kontoauszug01.txt"), hasNote("28.03.2011 - 30.04.2011 (2,400 %)"))));
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
                        hasSource("Kontoauszug02.txt"), hasNote("30.11.2012 - 01.12.2012 (2,150 %)"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2012-12-31"), hasAmount("EUR", 0.33), //
                        hasSource("Kontoauszug02.txt"), hasNote("01.12.2012 - 31.12.2012 (1,900 %)"))));
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
                        hasSource("Kontoauszug04.txt"), hasNote("03.07.2023 - 31.07.2023 (2,000 %)"))));
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
                        hasSource("Kontoauszug05.txt"), hasNote("30.11.2023 - 31.12.2023 (2,000 %)"))));
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
                        hasSource("Kontoauszug07.txt"), hasNote("Einzelüberweisung"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2012-11-30"), hasAmount("EUR", 2.71), //
                        hasSource("Kontoauszug07.txt"), hasNote("31.10.2012 - 30.11.2012 (2,150 %)"))));
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
                        hasSource("Kontoauszug08.txt"), hasNote("12.03.2021 - 12.09.2021 (0,300 %)"))));

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
                        hasSource("Kontoauszug09.txt"), hasNote("31.10.2022 - 11.11.2022 (0,300 %)"))));
    }

    @Test
    public void testKontoauszug10()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2013-12-03"), hasAmount("EUR", 150.00), //
                        hasSource("Kontoauszug10.txt"), hasNote("SEPA Gutschrift Bank"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2013-12-19"), hasAmount("EUR", 1824.71), //
                        hasSource("Kontoauszug10.txt"), hasNote("Einzelüberweisung"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2013-12-31"), hasAmount("EUR", 1.24), //
                        hasSource("Kontoauszug10.txt"), hasNote("30.11.2013 - 31.12.2013 (1,300 %)"))));
    }

    @Test
    public void testKontoauszug12()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-02-29"), hasAmount("EUR", 2.29), //
                        hasSource("Kontoauszug12.txt"), hasNote("SEPA Gutschrift"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-02-29"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug12.txt"), hasNote("31.01.2024 - 29.02.2024 (2,500 %)"))));
    }

    @Test
    public void testKontoauszug13()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-09-30"), hasAmount("EUR", 1.03), //
                        hasSource("Kontoauszug13.txt"), hasNote("31.08.2023 - 30.09.2023 (2,000 %)"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2023-09-30"), hasAmount("EUR", 0.25), //
                        hasSource("Kontoauszug13.txt"), hasNote("Abgeltungssteuer (1,03 EUR)"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2023-09-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug13.txt"), hasNote("Solidaritätszuschlag (0,25 EUR)"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2023-09-30"), hasAmount("EUR", 0.02), //
                        hasSource("Kontoauszug13.txt"), hasNote("Kirchensteuer (0,25 EUR)"))));
    }

    @Test
    public void testKontoauszug14()
    {
        AkfBankPDFExtractor extractor = new AkfBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-08-21"), hasAmount("EUR", 2.26), //
                        hasSource("Kontoauszug14.txt"), hasNote("30.12.2023 - 21.08.2024 (3,550 %)"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-08-21"), hasAmount("EUR", 1.26), //
                        hasSource("Kontoauszug14.txt"), hasNote("21.08.2023 - 30.12.2023 (3,550 %)"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2024-08-21"), hasAmount("EUR", 0.86), //
                        hasSource("Kontoauszug14.txt"), hasNote("Abgeltungssteuer (3,52 EUR)"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2024-08-21"), hasAmount("EUR", 0.05), //
                        hasSource("Kontoauszug14.txt"), hasNote("Solidaritätszuschlag (0,86 EUR)"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2024-08-21"), hasAmount("EUR", 0.07), //
                        hasSource("Kontoauszug14.txt"), hasNote("Kirchensteuer (0,86 EUR)"))));
    }
}
