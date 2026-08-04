package name.abuchen.portfolio.datatransfer.pdf.advanziabank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSkippedItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.AdvanziaBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class AdvanziaBankPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new AdvanziaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-14"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-15"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-16"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-20"), hasAmount("EUR", 9200.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-30"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-06-30"), hasAmount("EUR", 47.11), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new AdvanziaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-06"), hasAmount("EUR", 190.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-06"), hasAmount("EUR", 562.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-13"), hasAmount("EUR", 350.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-21"), hasAmount("EUR", 140.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-28"), hasAmount("EUR", 604.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-05-31"), hasAmount("EUR", 308.76), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new AdvanziaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-07-04"), hasAmount("EUR", 190.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-07-04"), hasAmount("EUR", 562.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-07-28"), hasAmount("EUR", 340.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2025-07-31"), hasAmount("EUR", 123.66), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));
    }

    @Test
    public void testKreditkartenabrechnung01()
    {
        var extractor = new AdvanziaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kreditkartenabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(16L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(16));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-07"), hasAmount("EUR", 13.45), //
                        hasSource("Kreditkartenabrechnung01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-22"), hasAmount("EUR", 17.96), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("A201 KF ARN APVuXQ LS - SEK 191,00 (KURS 10,6347) rXUoMInHk"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-25"), hasAmount("EUR", 4.26), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("sphb WUdh - MVR 77,00 (KURS 18,0751) gLHfjmKHv"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-25"), hasAmount("EUR", 3.41), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("dyw - USD 4,00 (KURS 1,1730) SnAGhsR"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-25"), hasAmount("EUR", 12.55), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("SIX-A - MVR 227,00 (KURS 18,0876) K."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-25"), hasAmount("EUR", 116.14), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("aQb679 - MVR 2100,00 (KURS 18,0816) QOG dAQ"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-26"), hasAmount("EUR", 30.01), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("kZXVf PoYk flG LTD - USD 35,19 (KURS 1,1726) gkbi"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-26"), hasAmount("EUR", 35.20), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("CayObxc LkVXuCi hYjovT - USD 41,27 (KURS 1,1724) daoQgrmRSX"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-26"), hasAmount("EUR", 282.06), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("ycwQcIfUNo TKzv MVR - MVR 5100,00 (KURS 18,0813) wHerhHrBfR"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-28"), hasAmount("EUR", 131.66), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("WwplbjFurJ hqzqldtZRlK - USD 156,00 (KURS 1,1849) qCBAkJfcdh"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-28"), hasAmount("EUR", 23.95), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("FGroaNr rUKeM JgY - USD 28,38 (KURS 1,1850) EIozMlVgtd"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-29"), hasAmount("EUR", 86.93), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("xDqUdwN XBmR zZaW - USD 103,60 (KURS 1,1918) ZaGi"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-30"), hasAmount("EUR", 21.44), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("wJV gcvfY bNV - USD 25,50 (KURS 1,1894) JDHnsUucja"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-30"), hasAmount("EUR", 27.38), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("ufQNAc cpLfn - MVR 502,25 (KURS 18,3437) UKhbzLbxOv"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-01-31"), hasAmount("EUR", 791.26), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote("DeLx WlbNr swdwsChxg - USD 939,30 (KURS 1,1871) hTIEYvMnjO"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2026-02-03"), //
                        hasSource("Kreditkartenabrechnung01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.11), hasGrossValue("EUR", 2.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKreditkartenabrechnung02()
    {
        var extractor = new AdvanziaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kreditkartenabrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-09"), hasAmount("EUR", 131.50), //
                        hasSource("Kreditkartenabrechnung02.txt"), //
                        hasNote("tfNYesQXO 16569540 jSiJCLzb"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-12"), hasAmount("EUR", 700.00), //
                        hasSource("Kreditkartenabrechnung02.txt"), //
                        hasNote("TOP cLu xwJDBZBkg fJGDD rYoFgv"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-19"), hasAmount("EUR", 700.00), //
                        hasSource("Kreditkartenabrechnung02.txt"), //
                        hasNote("TOP gZu SQlrWnllz JDrHc svoMNz"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-01"), hasAmount("EUR", 33.00), //
                        hasSource("Kreditkartenabrechnung02.txt"), //
                        hasNote("fdxIckwhI VKfH LEiv"))));
    }
}
