package name.abuchen.portfolio.datatransfer.pdf.scalablecapital;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.ScalableCapitalPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class ScalableCapitalPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0008T6IUX0"), hasWkn(null), hasTicker(null), //
                        hasName("Vngrd Fds-ESG Dv.As-Pc Al ETF"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-12T13:12:51"), hasShares(3.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Ord.-Nr.: SCALsin78vS5CYz"), //
                        hasAmount("EUR", 19.49), hasGrossValue("EUR", 18.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));

    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2903252349"), hasWkn(null), hasTicker(null), //
                        hasName("Scalable MSCI AC World Xtrackers (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-12-30T13:39:12"), hasShares(1.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ord.-Nr.: SCALRnomPwYQrc5"), //
                        hasAmount("EUR", 8.60), hasGrossValue("EUR", 9.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));

    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000HD5ZFL9"), hasWkn(null), hasTicker(null), //
                        hasName("Roche Hldg Long 227,26 CHF Turbo Open End HVB"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-05-12T09:01:04"), hasShares(200.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Ord.-Nr.: DbcK99JE4enn0td"), //
                        hasAmount("EUR", 472.00), hasGrossValue("EUR", 472.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB0007188757"), hasWkn(null), hasTicker(null), //
                        hasName("Rio Tinto PLC"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-05-12T09:48:14"), hasShares(5.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Ord.-Nr.: SCALzcysZPDAU8W"), //
                        hasAmount("EUR", 278.65), hasGrossValue("EUR", 278.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

    }

    @Test
    public void testSparplanausfuehrung01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplanausfuehrung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US0304201033"), hasWkn(null), hasTicker(null), //
                        hasName("American Water Works"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-27T11:46:01"), hasShares(0.016515), //
                        hasSource("Sparplanausfuehrung01.txt"), //
                        hasNote("Ord.-Nr.: SCALx2qhYduKQJf"), //
                        hasAmount("EUR", 2.00), hasGrossValue("EUR", 2.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

    }

    @Test
    public void testSparplanausfuehrung02()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplanausfuehrung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000EWG2LD7"), hasWkn(null), hasTicker(null), //
                        hasName("Boerse Stuttgart EUWAX Gold II"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-16T13:33:40"), hasShares(2.166255), //
                        hasSource("Sparplanausfuehrung02.txt"), //
                        hasNote("Ord.-Nr.: 000000000000000"), //
                        hasAmount("EUR", 200.00), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

    }

    @Test
    public void testSparplanausfuehrung03()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplanausfuehrung03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKX55R35"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE North America (Dist)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-16T13:49:30"), hasShares(3.85862), //
                        hasSource("Sparplanausfuehrung03.txt"), //
                        hasNote("Ord.-Nr.: 000000000000000"), //
                        hasAmount("EUR", 500.00), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

    }

    @Test
    public void testSavingsplan01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Savingsplan01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0908500753"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi Stoxx Europe 600 (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-05T12:25:16"), hasShares(22.650225), //
                        hasSource("Savingsplan01.txt"), //
                        hasNote("Order ID: BZzUZDlXBK5mNIc"), //
                        hasAmount("EUR", 2425.00), hasGrossValue("EUR", 2425.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

    }

    @Test
    public void testSavingsplan02()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Savingsplan02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BZCQB185"), hasWkn(null), hasTicker(null), //
                        hasName("iShares MSCI India (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-05T02:11:02"), hasShares(1.32005), //
                        hasSource("Savingsplan02.txt"), //
                        hasNote("Order ID: aYRCKHJ8ZrONSjY"), //
                        hasAmount("EUR", 85.65), hasGrossValue("EUR", 85.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

    }

    @Test
    public void testDividende01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US00123Q1040"), hasWkn(null), hasTicker(null), //
                        hasName("AGNC Investment Corp."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-01-15T00:00"), hasShares(0.663129), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.07), hasGrossValue("EUR", 0.08), //
                        hasForexGrossValue("USD", 0.08), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("AGNC Investment Corp.", "EUR");
        security.setIsin("US00123Q1040");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ScalableCapitalPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-01-15T00:00"), hasShares(0.663129), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.07), hasGrossValue("EUR", 0.08), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testRechnungsabschluss01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rechnungsabschluss01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-03-31T00:00"), hasShares(0.00), //
                        hasSource("Rechnungsabschluss01.txt"), //
                        hasNote("01.01.2025 - 31.03.2025"), //
                        hasAmount("EUR", 13.69), hasGrossValue("EUR", 13.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-04-04"), hasAmount("EUR", 4.99), //
                        hasSource("Kontoauszug01.txt"), hasNote("Prime-Abonnementgebühr"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-04-07"), hasAmount("EUR", 29715.63), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-04-09"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-04-14"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));
    }
}
