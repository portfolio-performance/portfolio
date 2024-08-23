package name.abuchen.portfolio.datatransfer.pdf.abnamrogroup;

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
import name.abuchen.portfolio.datatransfer.pdf.ABNAMROGroupPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class ABNAMROGroupPDFExtractorTest
{

    @Test
    public void testKontoauszug01()
    {
        ABNAMROGroupPDFExtractor extractor = new ABNAMROGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2011-10-19"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2011-10-27"), hasAmount("EUR", 2900.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2011-11-16"), hasAmount("EUR", 40500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        assertThat(results, hasItem(removal(hasDate("2011-12-16"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2011-12-21"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2011-12-23"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        assertThat(results, hasItem(interest(hasDate("2012-01-01"), hasAmount("EUR", 114.34), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug02()
    {
        ABNAMROGroupPDFExtractor extractor = new ABNAMROGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(25L));
        assertThat(results.size(), is(25));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(deposit(hasDate("2011-10-19"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2011-10-27"), hasAmount("EUR", 2900.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2011-11-16"), hasAmount("EUR", 40500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(removal(hasDate("2011-12-16"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2011-12-21"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2011-12-23"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(interest(hasDate("2012-01-01"), hasAmount("EUR", 114.34), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2012-02-24"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2012-02-27"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(removal(hasDate("2012-03-12"), hasAmount("EUR", 91.72), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(removal(hasDate("2012-03-22"), hasAmount("EUR", 406.88), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2012-03-29"), hasAmount("EUR", 384.26), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(interest(hasDate("2012-04-01"), hasAmount("EUR", 325.73), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(removal(hasDate("2012-04-11"), hasAmount("EUR", 50000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(interest(hasDate("2012-07-01"), hasAmount("EUR", 39.66), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2012-07-18"), hasAmount("EUR", 2400.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2012-08-01"), hasAmount("EUR", 234.61), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(removal(hasDate("2012-08-01"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(interest(hasDate("2012-10-01"), hasAmount("EUR", 3.01), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(interest(hasDate("2012-10-11"), hasAmount("EUR", 596.33), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(deposit(hasDate("2012-10-11"), hasAmount("EUR", 50000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(removal(hasDate("2012-12-17"), hasAmount("EUR", 45000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(taxes(hasDate("2013-01-01"), hasAmount("EUR", 49.74), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(taxes(hasDate("2013-01-01"), hasAmount("EUR", 2.73), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        assertThat(results, hasItem(interest(hasDate("2013-01-01"), hasAmount("EUR", 198.97), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug03()
    {
        ABNAMROGroupPDFExtractor extractor = new ABNAMROGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results, hasItem(interest(hasDate("2019-07-01"), hasAmount("EUR", 63.28), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));
    }

}
