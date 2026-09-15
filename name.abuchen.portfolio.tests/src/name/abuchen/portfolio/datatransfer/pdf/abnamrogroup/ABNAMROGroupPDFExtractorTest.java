package name.abuchen.portfolio.datatransfer.pdf.abnamrogroup;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSkippedItems;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.ABNAMROGroupPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class ABNAMROGroupPDFExtractorTest
{

    @Test
    public void testKontoauszug01()
    {
        var extractor = new ABNAMROGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-10-19"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-10-27"), hasAmount("EUR", 2900.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-11-16"), hasAmount("EUR", 40500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2011-12-16"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Zahlungsausgang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-12-21"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-12-23"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2012-01-01"), hasShares(0), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Ihre Tagesgeldzinsen"), //
                        hasAmount("EUR", 114.34), hasGrossValue("EUR", 114.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new ABNAMROGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(23L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(23));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-10-19"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-10-27"), hasAmount("EUR", 2900.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-11-16"), hasAmount("EUR", 40500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2011-12-16"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungsausgang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-12-21"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-12-23"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2012-01-01"), hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Ihre Tagesgeldzinsen"), //
                        hasAmount("EUR", 114.34), hasGrossValue("EUR", 114.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2012-02-24"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2012-02-27"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2012-03-12"), hasAmount("EUR", 91.72), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungsausgang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2012-03-22"), hasAmount("EUR", 406.88), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungsausgang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2012-03-29"), hasAmount("EUR", 384.26), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2012-04-01"), hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Ihre Tagesgeldzinsen"), //
                        hasAmount("EUR", 325.73), hasGrossValue("EUR", 325.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2012-04-11"), hasAmount("EUR", 50000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Abschluss eines Festgeldes"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2012-07-01"), hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Ihre Tagesgeldzinsen"), //
                        hasAmount("EUR", 39.66), hasGrossValue("EUR", 39.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2012-07-18"), hasAmount("EUR", 2400.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2012-08-01"), hasAmount("EUR", 234.61), //
                        hasSource("Kontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2012-08-01"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Abschluss eines Festgeldes"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2012-10-01"), hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Ihre Tagesgeldzinsen"), //
                        hasAmount("EUR", 3.01), hasGrossValue("EUR", 3.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2012-10-11"), hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Zinszahlung Festgeld"), //
                        hasAmount("EUR", 596.33), hasGrossValue("EUR", 596.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2012-10-11"), hasAmount("EUR", 50000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Rückzahlung Ihres Festgeldes"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2012-12-17"), hasAmount("EUR", 45000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Abschluss eines Festgeldes"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2013-01-01"), hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Ihre Tagesgeldzinsen"), //
                        hasAmount("EUR", 198.97), hasGrossValue("EUR", 251.44), //
                        hasTaxes("EUR", 49.74 + 2.73), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new ABNAMROGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2019-07-01"), hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Ihre Zinsabrechnung"), //
                        hasAmount("EUR", 63.28), hasGrossValue("EUR", 63.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new ABNAMROGroupPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(8L));
        assertThat(countAccountTransactions(results), is(16L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(25));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("098020"), hasTicker(null), //
                        hasName("VANG FTSE WLD"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-03-02T00:00"), hasShares(1.7308), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 251.00), hasGrossValue("EUR", 251.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-04-02T00:00"), hasShares(1.8034), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 251.00), hasGrossValue("EUR", 251.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-04T00:00"), hasShares(1.6605), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 251.00), hasGrossValue("EUR", 251.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-06-02T00:00"), hasShares(1.5709), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 251.00), hasGrossValue("EUR", 251.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-08-19T00:00"), hasShares(1.8585), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 300.00), hasGrossValue("EUR", 300.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-08-25T00:00"), hasShares(1.2410), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 200.00), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-08-26T00:00"), hasShares(1.2392), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 200.00), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-08-28T00:00"), hasShares(1.3808), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 224.35), hasGrossValue("EUR", 224.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-03-31"), hasExDate(null), //
                        hasShares(1.7308), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.69), hasGrossValue("EUR", 0.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-06-30"), hasExDate(null), //
                        hasShares(6.7656), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.37), hasGrossValue("EUR", 5.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-03-01"), hasAmount("EUR", 251.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("SEPA Overboeking"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-04-01"), hasAmount("EUR", 251.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("SEPA Overboeking"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-01"), hasAmount("EUR", 251.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("SEPA Overboeking"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-06-01"), hasAmount("EUR", 251.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("SEPA Overboeking"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-06-12"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("SEPA Overboeking"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-08-19"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("SEPA Overboeking"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-08-25"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("SEPA Overboeking"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-08-26"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("SEPA Overboeking"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-08-27"), hasAmount("EUR", 224.35), //
                        hasSource("Kontoauszug04.txt"), hasNote("SEPA Overboeking"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-06-16"), hasAmount("EUR", 90.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("SEPA Overboeking"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-07-16"), hasAmount("EUR", 11.15), //
                        hasSource("Kontoauszug04.txt"), hasNote("SEPA Overboeking"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2026-04-02"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("from 31.12.2025 to 31.03.2026"), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2026-07-02"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("from 31.03.2026 to 30.06.2026"), //
                        hasAmount("EUR", 0.12), hasGrossValue("EUR", 0.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2026-06-12"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("ABNAMRO Investments"), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
