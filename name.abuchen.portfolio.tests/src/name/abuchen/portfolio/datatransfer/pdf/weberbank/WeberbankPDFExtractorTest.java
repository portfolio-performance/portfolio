package name.abuchen.portfolio.datatransfer.pdf.weberbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.WeberbankPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class WeberbankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new WeberbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NO0010081235"), hasWkn("A0B733"), hasTicker(null), //
                        hasName("NEL ASA NAVNE-AKSJER NK -,20"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-26T15:14:29"), hasShares(4440.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Limit billigst"), //
                        hasAmount("EUR", 9657.00), hasGrossValue("EUR", 9657.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new WeberbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NO0010875230"), hasWkn("A28TXS"), hasTicker(null), //
                        hasName("1,375 % NORWEGEN, KÖNIGREICH NK-ANL. 2020(30)"), //
                        hasCurrencyCode("NOK"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-04-02T10:48:46"), hasShares(14250.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer 505279/90.00 | Limit 88,50 % | Stückzinsen für 232 Tage: 1.111,28 EUR"), //
                        hasAmount("EUR", 113295.54), hasGrossValue("EUR", 113295.54), //
                        hasForexGrossValue("NOK", 1269703.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-04-02T10:48:46"), hasShares(14250.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer 505279/90.00 | Limit 88,50 % | Stückzinsen für 232 Tage: 1.111,28 EUR"), //
                        hasAmount("EUR", 293.10), hasGrossValue("EUR", 293.10), //
                        hasForexGrossValue("NOK", 3284.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf02WithSecurityInEUR()
    {
        var security = new Security("1,375 % NORWEGEN, KÖNIGREICH NK-ANL. 2020(30)", "EUR");
        security.setIsin("NO0010875230");
        security.setWkn("A28TXS");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WeberbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-04-02T10:48:46"), hasShares(14250.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer 505279/90.00 | Limit 88,50 % | Stückzinsen für 232 Tage: 1.111,28 EUR"), //
                        hasAmount("EUR", 113295.54), hasGrossValue("EUR", 113295.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-04-02T10:48:46"), hasShares(14250.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer 505279/90.00 | Limit 88,50 % | Stückzinsen für 232 Tage: 1.111,28 EUR"), //
                        hasAmount("EUR", 293.10), hasGrossValue("EUR", 293.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new WeberbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US92556V1061"), hasWkn("A2QAME"), hasTicker(null), //
                        hasName("VIATRIS INC. REGISTERED SHARES O.N."), //
                        hasCurrencyCode("EUR"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-03-26T15:12:58"), hasShares(193.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer 742198/43.00 | Limit bestens"), //
                        hasAmount("EUR", 2335.30), hasGrossValue("EUR", 2335.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new WeberbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9024941034"), hasWkn("870625"), hasTicker(null), //
                        hasName("TYSON FOODS INC. REG. SHARES CL.A DL -,10"), //
                        hasCurrencyCode("EUR"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-06-18T17:43:22"), hasShares(465.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftragsnummer 673747/44.00 | Limit bestens"), //
                        hasAmount("EUR", 22719.90), hasGrossValue("EUR", 22719.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-06-18T17:43:22"), hasShares(465.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftragsnummer 673747/44.00 | Limit bestens"), //
                        hasAmount("EUR", 558.03), hasGrossValue("EUR", 558.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new WeberbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn("865985"), hasTicker(null), //
                        hasName("APPLE INC. REGISTERED SHARES O.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-13T00:00"), hasExDate("2020-08-07T00:00"), //
                        hasShares(107.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Quartalsdividende"), //
                        hasAmount("EUR", 55.14), hasGrossValue("EUR", 74.05), //
                        hasForexGrossValue("USD", 87.74), //
                        hasTaxes("EUR", 11.11 + 7.40 + 0.40), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("APPLE INC. REGISTERED SHARES O.N.", "EUR");
        security.setIsin("US0378331005");
        security.setWkn("865985");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WeberbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-13T00:00"), hasExDate("2020-08-07T00:00"), //
                        hasShares(107.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Quartalsdividende"), //
                        hasAmount("EUR", 55.14), hasGrossValue("EUR", 74.05), //
                        hasTaxes("EUR", 11.11 + 7.40 + 0.40), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new WeberbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0010273215"), hasWkn("A1J4U4"), hasTicker(null), //
                        hasName("ASML HOLDING N.V. AANDELEN OP NAAM EO -,09"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-02-18T00:00"), hasExDate("2026-02-09T00:00"), //
                        hasShares(65.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungsnr. 54861333620 | Schlussdividende"), //
                        hasAmount("EUR", 77.43), hasGrossValue("EUR", 104.00), //
                        hasTaxes("EUR", 15.60 + 10.40 + 0.57), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new WeberbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DK0062498333"), hasWkn("A3EU6F"), hasTicker(null), //
                        hasName("NOVO-NORDISK AS NAVNE-AKTIER B DK 0,1"), //
                        hasCurrencyCode("DKK"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-01T00:00"), hasExDate("2025-03-28T00:00"), //
                        hasShares(550.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr. 59465024890"), //
                        hasAmount("EUR", 362.67), hasGrossValue("EUR", 580.73), //
                        hasForexGrossValue("DKK", 4345.00), //
                        hasTaxes("EUR", 156.80 + 58.07 + 3.19), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("NOVO-NORDISK AS NAVNE-AKTIER B DK 0,1", "EUR");
        security.setIsin("DK0062498333");
        security.setWkn("A3EU6F");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WeberbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-01T00:00"), hasExDate("2025-03-28T00:00"), //
                        hasShares(550.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr. 59465024890"), //
                        hasAmount("EUR", 362.67), hasGrossValue("EUR", 580.73), //
                        hasTaxes("EUR", 156.80 + 58.07 + 3.19), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new WeberbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NO0010875230"), hasWkn("A28TXS"), hasTicker(null), //
                        hasName("NORWEGEN, KÖNIGREICH NK-ANL. 2020(30)"), //
                        hasCurrencyCode("NOK"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-08-19T00:00"), hasExDate(null), //
                        hasShares(14250.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 73164322720"), //
                        hasAmount("EUR", 1319.35), hasGrossValue("EUR", 1791.99), //
                        hasForexGrossValue("NOK", 19593.75), //
                        hasTaxes("EUR", 448.00 + 24.64), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("NORWEGEN, KÖNIGREICH NK-ANL. 2020(30)", "EUR");
        security.setIsin("NO0010875230");
        security.setWkn("A28TXS");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WeberbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-08-19T00:00"), hasExDate(null), //
                        hasShares(14250.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 73164322720"), //
                        hasAmount("EUR", 1319.35), hasGrossValue("EUR", 1791.99), //
                        hasTaxes("EUR", 448.00 + 24.64), hasFees("EUR", 0.00))));
    }
}
