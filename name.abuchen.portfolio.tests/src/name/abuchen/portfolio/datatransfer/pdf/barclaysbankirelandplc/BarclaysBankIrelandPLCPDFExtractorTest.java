package name.abuchen.portfolio.datatransfer.pdf.barclaysbankirelandplc;

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
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.BarclaysBankIrelandPLCPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class BarclaysBankIrelandPLCPDFExtractorTest
{
    @Test
    public void testKreditKontoauszug01()
    {
        var extractor = new BarclaysBankIrelandPLCPDFExtractor(new Client());

        var errors = new ArrayList<Exception>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug01.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2023-11-20"), hasAmount("EUR", 119.96), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("GetYourGuide Tickets Berlin"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-11-20"), hasAmount("EUR", 21.84), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("ALDI ALBUFEIRA ALBUFEIRA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-11-20"), hasAmount("EUR", 8.00), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("GRUPO PESTANA ALCACER DO SA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-01"), hasAmount("EUR", 34.99), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("eBay O*00-00000-00000 Luxembourg"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-06"), hasAmount("EUR", 1.00), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("PAYPAL *IONOS SE 00000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-09"), hasAmount("EUR", 8.48), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("Globus Baumarkt Ort"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-09"), hasAmount("EUR", 1.29), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("Tegut Filiale 0000 LangOrtsnamen"))));
    }

    @Test
    public void testKreditKontoauszug02()
    {
        var extractor = new BarclaysBankIrelandPLCPDFExtractor(new Client());

        var errors = new ArrayList<Exception>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-04"), hasAmount("EUR", 4.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("TooGoodToG xxxxxxxxxxx toogoodtogo.d"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-05"), hasAmount("EUR", 1.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("PAYPAL *IONOS SE 00000000000"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-25"), hasAmount("EUR", 60.78), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("Lidl sagt Danke Ort"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-25"), hasAmount("EUR", 60.78), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("Lidl sagt Danke Ort"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-12-28"), hasAmount("EUR", 60.78), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("Lidl sagt Danke Ort"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-05"), hasAmount("EUR", 5.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("PAYPAL *VODAFONE 0000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-08"), hasAmount("EUR", 5.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("PAYPAL *VODAFONE 0000000000"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-06"), hasAmount("EUR", 671.99), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("Per Lastschrift dankend erhalten"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-08"), hasAmount("EUR", 1.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("Vorname Nachname"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-08"), hasAmount("EUR", 0.50), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("Gutschrift Manuelle Lastschrift"))));
    }

    @Test
    public void testKreditKontoauszug03()
    {
        var extractor = new BarclaysBankIrelandPLCPDFExtractor(new Client());

        var errors = new ArrayList<Exception>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug03.txt"), errors);

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
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-31"), //
                        hasSource("KreditKontoauszug03.txt"), //
                        hasNote("Habenzinsen"), //
                        hasAmount("EUR", 3.54), hasGrossValue("EUR", 4.80), //
                        hasTaxes("EUR", 1.20 + 0.06), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-31"), //
                        hasSource("KreditKontoauszug03.txt"), //
                        hasNote("Habenzinsen"), //
                        hasAmount("EUR", 14.47), hasGrossValue("EUR", 19.65), //
                        hasTaxes("EUR", 4.91 + 0.27), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-31"), //
                        hasSource("KreditKontoauszug03.txt"), //
                        hasNote("Habenzinsen"), //
                        hasAmount("EUR", 16.41), hasGrossValue("EUR", 22.28), //
                        hasTaxes("EUR", 5.57 + 0.30), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-31"), //
                        hasSource("KreditKontoauszug03.txt"), //
                        hasNote("Habenzinsen"), //
                        hasAmount("EUR", 103.38), hasGrossValue("EUR", 140.41), //
                        hasTaxes("EUR", 35.10 + 1.93), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKreditKontoauszug04()
    {
        var extractor = new BarclaysBankIrelandPLCPDFExtractor(new Client());

        var errors = new ArrayList<Exception>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(26L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(26));
        new AssertImportActions().check(results, "EUR");

        // assert transactions
        assertThat(results, hasItem(removal(hasDate("2020-07-20"), hasAmount("EUR", 89.94), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop Umsatz 123-456-7890"))));
        assertThat(results, hasItem(removal(hasDate("2020-07-20"), hasAmount("EUR", 45.00), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("LADEN1 MUENCHEN"))));
        assertThat(results, hasItem(deposit(hasDate("2020-07-20"), hasAmount("EUR", 187.04), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop3 Umsatz Luxembourg"))));
        assertThat(results, hasItem(removal(hasDate("2020-07-20"), hasAmount("EUR", 38.80), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop2 Umsatz EXAMPLE.COM"))));
        assertThat(results, hasItem(removal(hasDate("2020-07-21"), hasAmount("EUR", 40.89), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop2 Umsatz EXAMPLE.COM"))));
        assertThat(results, hasItem(removal(hasDate("2020-07-22"), hasAmount("EUR", 58.99), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop Umsatz 123-456-7890"))));
        assertThat(results, hasItem(removal(hasDate("2020-07-23"), hasAmount("EUR", 7.61), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("LADEN2 MÜNCHEN"))));
        assertThat(results, hasItem(removal(hasDate("2020-07-24"), hasAmount("EUR", 50.77), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop Umsatz 123-456-7890"))));
        assertThat(results, hasItem(removal(hasDate("2020-07-25"), hasAmount("EUR", 32.75), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop Umsatz 123-456-7890"))));
        assertThat(results, hasItem(removal(hasDate("2020-07-25"), hasAmount("EUR", 18.69), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("LADEN2 MÜNCHEN"))));
        assertThat(results, hasItem(removal(hasDate("2020-07-27"), hasAmount("EUR", 17.40), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("LADEN3 MUENCHEN"))));
        assertThat(results, hasItem(removal(hasDate("2020-07-27"), hasAmount("EUR", 18.61), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("LADEN2 MÜNCHEN"))));
        assertThat(results, hasItem(removal(hasDate("2020-07-31"), hasAmount("EUR", 96.45), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("LADEN 3 weitweg Eching"))));
        assertThat(results, hasItem(removal(hasDate("2020-07-31"), hasAmount("EUR", 50.95), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Laden 4 ECHING"))));
        assertThat(results, hasItem(removal(hasDate("2020-08-03"), hasAmount("EUR", 19.49), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Laden 5 Muenchen"))));
        assertThat(results, hasItem(removal(hasDate("2020-08-03"), hasAmount("EUR", 9.80), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("LADEN2 MÜNCHEN"))));
        assertThat(results, hasItem(removal(hasDate("2020-08-05"), hasAmount("EUR", 38.98), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop2 Umsatz EXAMPLE.COM"))));
        assertThat(results, hasItem(removal(hasDate("2020-08-05"), hasAmount("EUR", 12.42), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop2 Umsatz EXAMPLE.COM"))));
        assertThat(results, hasItem(removal(hasDate("2020-08-07"), hasAmount("EUR", 38.70), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop Umsatz 123-456-7890"))));
        assertThat(results, hasItem(removal(hasDate("2020-08-10"), hasAmount("EUR", 13.21), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Laden 6 MUENCHEN"))));
        assertThat(results, hasItem(removal(hasDate("2020-08-10"), hasAmount("EUR", 22.91), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Laden 4 ECHING"))));
        assertThat(results, hasItem(removal(hasDate("2020-08-10"), hasAmount("EUR", 73.77), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop2 Umsatz EXAMPLE.COM"))));
        assertThat(results, hasItem(removal(hasDate("2020-08-10"), hasAmount("EUR", 24.49), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("LADEN2 MÜNCHEN"))));
        assertThat(results, hasItem(removal(hasDate("2020-08-12"), hasAmount("EUR", 19.99), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop Umsatz 123-456-7890"))));
        assertThat(results, hasItem(deposit(hasDate("2020-08-12"), hasAmount("EUR", 19.98), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop Umsatz 123-456-7890"))));
        assertThat(results, hasItem(removal(hasDate("2020-08-18"), hasAmount("EUR", 119.22), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Onlineshop2 Umsatz EXAMPLE.COM"))));
    }

    @Test
    public void testKreditKontoauszug05()
    {
        var extractor = new BarclaysBankIrelandPLCPDFExtractor(new Client());

        var errors = new ArrayList<Exception>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // assert transactions
        assertThat(results, hasItem(removal(hasDate("2015-06-27"), hasAmount("EUR", 95.91), //
                        hasSource("KreditKontoauszug05.txt"), //
                        hasNote("Shell 7730 Muenchen"))));
        assertThat(results, hasItem(removal(hasDate("2015-06-27"), hasAmount("EUR", 454.39), //
                        hasSource("KreditKontoauszug05.txt"), //
                        hasNote("HOTELRES123123456789 11234567895"))));
        assertThat(results, hasItem(removal(hasDate("2015-07-01"), hasAmount("EUR", 18.00), //
                        hasSource("KreditKontoauszug05.txt"), //
                        hasNote("ASFINAG.AT VIDEOMAUT WIEN"))));
        assertThat(results, hasItem(removal(hasDate("2015-07-02"), hasAmount("EUR", 56.60), //
                        hasSource("KreditKontoauszug05.txt"), //
                        hasNote("AUTOST BRENNERO/FROSIN TOLL WAY"))));
        assertThat(results, hasItem(removal(hasDate("2015-07-14"), hasAmount("EUR", 18.99), //
                        hasSource("KreditKontoauszug05.txt"), //
                        hasNote("Amazon EU AMAZON.DE"))));
        assertThat(results, hasItem(removal(hasDate("2015-07-15"), hasAmount("EUR", 5.00), //
                        hasSource("KreditKontoauszug05.txt"), //
                        hasNote("PARCHEGGI FIUMICINO AD FIUMICINO"))));
        assertThat(results, hasItem(removal(hasDate("2015-07-17"), hasAmount("EUR", 26.49), //
                        hasSource("KreditKontoauszug05.txt"), //
                        hasNote("Amazon EU AMAZON.DE"))));
        assertThat(results, hasItem(removal(hasDate("2015-07-18"), hasAmount("EUR", 56.60), //
                        hasSource("KreditKontoauszug05.txt"), //
                        hasNote("A 22 BRENNERO BARRIERA VIPITENO"))));
        assertThat(results, hasItem(removal(hasDate("2015-07-19"), hasAmount("EUR", 31.00), //
                        hasSource("KreditKontoauszug05.txt"), //
                        hasNote("Reiseversicherung jährlicher Abschluss"))));
        assertThat(results, hasItem(deposit(hasDate("2015-07-08"), hasAmount("EUR", 2000.00), //
                        hasSource("KreditKontoauszug05.txt"), //
                        hasNote("Vorname Nachname"))));
    }

    @Test
    public void testKreditKontoauszug06()
    {
        var extractor = new BarclaysBankIrelandPLCPDFExtractor(new Client());

        var errors = new ArrayList<Exception>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transactions
        assertThat(results, hasItem(removal(hasDate("2021-02-19"), hasAmount("EUR", 10.16), //
                        hasSource("KreditKontoauszug06.txt"), //
                        hasNote("Laden 1 Filiale Muenchen"))));
        assertThat(results, hasItem(removal(hasDate("2021-02-19"), hasAmount("EUR", 25.90), //
                        hasSource("KreditKontoauszug06.txt"), //
                        hasNote("Laden 1 Filiale Muenchen"))));
        assertThat(results, hasItem(removal(hasDate("2021-03-15"), hasAmount("EUR", 33.91), //
                        hasSource("KreditKontoauszug06.txt"), //
                        hasNote("Laden 2 Filiale Muenchen"))));
        assertThat(results, hasItem(removal(hasDate("2021-03-17"), hasAmount("EUR", 39.45), //
                        hasSource("KreditKontoauszug06.txt"), //
                        hasNote("Laden 3 Filiale ECHING"))));
        assertThat(results, hasItem(removal(hasDate("2021-03-18"), hasAmount("EUR", 14.92), //
                        hasSource("KreditKontoauszug06.txt"), //
                        hasNote("Laden 4 Filiale POECKING"))));
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2021-03-18"), //
                        hasSource("KreditKontoauszug06.txt"), //
                        hasNote("Monatl. Zinsen für Einkäufe/Überweisungen"), //
                        hasAmount("EUR", 1.68), hasGrossValue("EUR", 1.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
