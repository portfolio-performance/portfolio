package name.abuchen.portfolio.datatransfer.pdf.raiffeisenbankgruppe;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
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

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.RaiffeisenBankgruppePDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;

@SuppressWarnings("nls")
public class RaiffeisenbankgruppePDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

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
                        hasIsin("DE000BAY0017"), hasWkn(null), hasTicker(null), //
                        hasName("Bayer AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03T13:45:18"), hasShares(2.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftrags-Nr.: 11441163"), //
                        hasAmount("EUR", 107.26), hasGrossValue("EUR", 106.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.32))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

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
                        hasIsin("US7475251036"), hasWkn("883121"), hasTicker(null), //
                        hasName("QUALCOMM INC. REGISTERED SHARES DL -,0001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-11-09T09:58:45"), hasShares(100.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer 338500/63.00 | Limit billigst"), //
                        hasAmount("EUR", 14399.34), hasGrossValue("EUR", 14368.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 28.74 + 2.50 + 0.10))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

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
                        hasIsin("IE00B6YX5C33"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR S&P 500 UCITS ETF Registered Shares USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-12T16:42:58"), hasShares(6.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftrags-Nr.: 12345678"), //
                        hasAmount("EUR", 2510.00), hasGrossValue("EUR", 2478.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.00 + 25.00 + 3.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

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
                        hasIsin("CA1363851017"), hasWkn(null), hasTicker(null), //
                        hasName("Canadian Natural Resources Ltd Registered Shares o.N."), //
                        hasCurrencyCode("CAD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-19T15:30"), hasShares(22.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftrags-Nr.: 12345678"), //
                        hasAmount("EUR", 1121.40), hasGrossValue("EUR", 1041.48), //
                        hasForexGrossValue("CAD", 1464.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (73.00 / 1.406) + 25.00 + 3.00))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInEUR()
    {
        var security = new Security("Canadian Natural Resources Ltd Registered Shares o.N.", "EUR");
        security.setIsin("CA1363851017");
        security.setWkn("884437");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-19T15:30"), hasShares(22.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftrags-Nr.: 12345678"), //
                        hasAmount("EUR", 1121.40), hasGrossValue("EUR", 1041.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (73.00 / 1.406) + 25.00 + 3.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

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
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE All-World U.ETF Reg. Shs USD Acc. oN"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-03-24T12:20:52"), hasShares(200.00), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Auftrags-Nr.: 44083824"), //
                        hasAmount("EUR", 18521.34), hasGrossValue("EUR", 18472.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 18.47 + 30.87))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

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
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE All-World U.ETF Reg. Shs USD Acc. oN"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-03-06T10:46:12"), hasShares(100.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Auftrags-Nr.: 41601727"), //
                        hasAmount("EUR", 9681.58), hasGrossValue("EUR", 9652.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.65 + 19.93))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0102295455"), hasWkn("10229545"), hasTicker(null), //
                        hasName("Raiffeisen Futura - Pension Invest Balanced -V-"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-21T00:00"), hasShares(2.549), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Ref.-Nr.: 13819098307"), //
                        hasAmount("CHF", 399.95), hasGrossValue("CHF", 396.78), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 3.17))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

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
                        hasIsin("IE00B14X4S71"), hasWkn("A0J202"), hasTicker(null), //
                        hasName("ISHS DL TREAS.BD 1-3YR U.ETF REGISTERED SHARES USD (DIST)ON"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-15T09:33:55"), hasShares(0.859), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Auftragsnummer 464088/93.00"), //
                        hasAmount("EUR", 101.50), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.50))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

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
                        hasIsin("LU0478205379"), hasWkn("DBX0EY"), hasTicker(null), //
                        hasName("XTRACKERS II EUR CORPORATE BD INHABER-ANTEILE 1C O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-05T15:38:26"), hasShares(35.00), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Auftragsnummer 464088/93.00 | Limit billigst"), //
                        hasAmount("EUR", 5007.32), hasGrossValue("EUR", 5002.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.95))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0037430946"), hasWkn("3743094"), hasTicker(null), //
                        hasName("Swisscanto (CH) Real Estate Fund Responsible IFCA"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-14T00:00"), hasShares(27.00), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Ref.-Nr.: 17815533192"), //
                        hasAmount("CHF", 5081.10), hasGrossValue("CHF", 4995.00), //
                        hasTaxes("CHF", 3.75), hasFees("CHF", 2.35 + 80.00))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

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
                        hasIsin("AT0000A1TX20"), hasWkn(null), hasTicker(null), //
                        hasName("Raiffeisen-MegaTr.-ESG Aktien(RZ) T Miteigentumsanteile - Thesaurierend"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-20T00:00"), hasShares(0.256), //
                        hasSource("Kauf11.txt"), //
                        hasNote("Dauerauftrag-Nr.: 51"), //
                        hasAmount("EUR", 50.05), hasGrossValue("EUR", 47.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.14 + 0.95))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf12.txt"), errors);

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
                        hasIsin("AT0000A2DFK8"), hasWkn(null), hasTicker(null), //
                        hasName("Raiff.-SmartEnergy-ESG-Aktien(RZ)T Miteigentumsanteile - Thesaurierend"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-20T00:00"), hasShares(1.395), //
                        hasSource("Kauf12.txt"), //
                        hasNote("Dauerauftrag-Nr.: 54"), //
                        hasAmount("EUR", 199.90), hasGrossValue("EUR", 194.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.17 + 3.88))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

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
                        hasIsin("AT0000837307"), hasWkn(null), hasTicker(null), //
                        hasName("Zumtobel Group AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-04-07T11:17:43"), hasShares(4500.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftrags-Nr.: 21112411"), //
                        hasAmount("EUR", 36115.76), hasGrossValue("EUR", 36923.18), //
                        hasTaxes("EUR", 696.65), hasFees("EUR", 110.77))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

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
                        hasIsin("US09075V1026"), hasWkn("A2PSR2"), hasTicker(null), //
                        hasName("BIONTECH SE NAM.-AKT.(SP.ADRS)1/O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-07-06T11:39:42"), hasShares(110.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftragsnummer 183260/06.00 | Limit bestens"), //
                        hasAmount("EUR", 19966.38), hasGrossValue("EUR", 20009.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 40.02 + 2.50 + 0.10))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

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
                        hasIsin("US5949181045"), hasWkn("870747"), hasTicker(null), //
                        hasName("MICROSOFT CORP. REGISTERED SHARES DL-,00000625"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-04-01T11:23:39"), hasShares(15.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftragsnummer 554374/09.00 | Limit bestens"), //
                        hasAmount("EUR", 4187.16), hasGrossValue("EUR", 4197.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.49 + 0.10))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

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
                        hasIsin("DE0009848119"), hasWkn(null), hasTicker(null), //
                        hasName("DWS Top Dividende Inhaber-Anteile LD"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-03-27T00:00"), hasShares(200.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftrags-Nr.: 47199493"), //
                        hasAmount("EUR", 26000.00), hasGrossValue("EUR", 26000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

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
                        hasIsin("DE000BAY0017"), hasWkn(null), hasTicker(null), //
                        hasName("Bayer AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-04-30T00:00"), hasExDate("2021-04-28T00:00"), //
                        hasShares(90.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Ref.-Nr.: 19110111"), //
                        hasAmount("EUR", 110.02), hasGrossValue("EUR", 180.00), //
                        hasTaxes("EUR", 47.48 + 22.50), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

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
                        hasIsin("US7475251036"), hasWkn("883121"), hasTicker(null), //
                        hasName("QUALCOMM INC. REGISTERED SHARES DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-12-20T00:00"), hasExDate("2021-12-01T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungsnr. 85127406360 | Quartalsdividende"), //
                        hasAmount("EUR", 50.88), hasGrossValue("EUR", 59.86), //
                        hasForexGrossValue("USD", 68.00), //
                        hasTaxes("EUR", 8.98), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        var security = new Security("QUALCOMM INC. REGISTERED SHARES DL -,0001", "EUR");
        security.setIsin("US7475251036");
        security.setWkn("883121");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

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
                        hasDate("2021-12-20T00:00"), hasExDate("2021-12-01T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungsnr. 85127406360 | Quartalsdividende"), //
                        hasAmount("EUR", 50.88), hasGrossValue("EUR", 59.86), //
                        hasTaxes("EUR", 8.98), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

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
                        hasIsin("US17275R1023"), hasWkn("878841"), hasTicker(null), //
                        hasName("CISCO SYSTEMS INC.  SHARES REGISTERED SHARES DL-,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-10-28T00:00"), hasExDate("2022-10-04T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr. 88888888888 | Quartalsdividende"), //
                        hasAmount("EUR", 27.84), hasGrossValue("EUR", 37.68), //
                        hasForexGrossValue("USD", 38.00), //
                        hasTaxes("EUR", 5.65 + 3.69 + 0.20 + 0.30), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("CISCO SYSTEMS INC.  SHARES REGISTERED SHARES DL-,001", "EUR");
        security.setIsin("US17275R1023");
        security.setWkn("878841");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new RaiffeisenBankgruppePDFExtractor(client);

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
                        hasDate("2022-10-28T00:00"), hasExDate("2022-10-04T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr. 88888888888 | Quartalsdividende"), //
                        hasAmount("EUR", 27.84), hasGrossValue("EUR", 37.68), //
                        hasTaxes("EUR", 5.65 + 3.69 + 0.20 + 0.30), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

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
                        hasIsin("US3703341046"), hasWkn("853862"), hasTicker(null), //
                        hasName("GENERAL MILLS INC. REGISTERED SHARES DL -,10"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-11-03T00:00"), hasExDate("2022-10-06T00:00"), //
                        hasShares(400.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 80642931040 | Quartalsdividende"), //
                        hasAmount("EUR", 127.60), hasGrossValue("EUR", 217.00), //
                        hasForexGrossValue("USD", 216.00), //
                        hasTaxes("EUR", 65.10 + 21.22 + 1.17 + 1.91), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("GENERAL MILLS INC. REGISTERED SHARES DL -,10", "EUR");
        security.setIsin("US3703341046");
        security.setWkn("853862");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new RaiffeisenBankgruppePDFExtractor(client);

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
                        hasDate("2022-11-03T00:00"), hasExDate("2022-10-06T00:00"), //
                        hasShares(400.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 80642931040 | Quartalsdividende"), //
                        hasAmount("EUR", 127.60), hasGrossValue("EUR", 217.00), //
                        hasTaxes("EUR", 65.10 + 21.22 + 1.17 + 1.91), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

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
                        hasIsin("DE000A0D8Q49"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-01-17T00:00"), hasExDate("2023-01-17T00:00"), //
                        hasShares(221.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Ref.-Nr.: 47467310"), //
                        hasAmount("EUR", 97.63), hasGrossValue("EUR", 109.96), //
                        hasForexGrossValue("USD", 119.37), //
                        hasTaxes("EUR", 10.00 + 0.29 + (0.64 / 1.0856)), hasFees("EUR", 1.45))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        var security = new Security("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile", "EUR");
        security.setIsin("DE000A0D8Q49");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

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
                        hasDate("2023-01-17T00:00"), hasExDate("2023-01-17T00:00"), //
                        hasShares(221.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Ref.-Nr.: 47467310"), //
                        hasAmount("EUR", 97.63), hasGrossValue("EUR", 109.96), //
                        hasTaxes("EUR", 10.00 + 0.29 + (0.64 / 1.0856)), hasFees("EUR", 1.45), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

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
                        hasIsin("DE0007664039"), hasWkn(null), hasTicker(null), //
                        hasName("VOLKSWAGEN AG VORZUGSAKTIEN O.ST. O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-01-09T00:00"), hasExDate("2022-12-19T00:00"), //
                        hasShares(70.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Ref.-Nr.: 42008422"), //
                        hasAmount("EUR", 813.78), hasGrossValue("EUR", 1334.20), //
                        hasTaxes("EUR", 351.90 + 166.78 + 0.29), hasFees("EUR", 1.45))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

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
                        hasIsin("DE0005557508"), hasWkn("555750"), hasTicker(null), //
                        hasName("DEUTSCHE TELEKOM AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-04-12T00:00"), hasExDate("2023-04-06T00:00"), //
                        hasShares(111.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Abrechnungsnr. 66666666666"), //
                        hasAmount("EUR", 77.70), hasGrossValue("EUR", 77.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

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
                        hasIsin("LU0486851024"), hasWkn(null), hasTicker(null), //
                        hasName("Xtrackers MSCI Europe Value Inhaber-Anteile 1C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2022-07-07T00:00"), hasExDate(null), //
                        hasShares(800.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Ref.-Nr.: 46102104 | Ausschüttungsgleicher Ertrag"), //
                        hasAmount("EUR", 156.32), hasGrossValue("EUR", 156.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

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
                        hasIsin("DE000A0D8Q49"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-04-15T00:00"), hasExDate("2020-04-15T00:00"), //
                        hasShares(221.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Ref.-Nr.: 41638275"), //
                        hasAmount("EUR", 82.99), hasGrossValue("EUR", 97.81), //
                        hasForexGrossValue("USD", 106.58), //
                        hasTaxes("EUR", (0.86 / 1.0897) + 9.29 + 0.29), hasFees("EUR", 1.45 + 3.00))));
    }

    @Test
    public void testDividende09WithSecurityInEUR()
    {
        var security = new Security("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile", "EUR");
        security.setIsin("DE000A0D8Q49");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

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
                        hasDate("2020-04-15T00:00"), hasExDate("2020-04-15T00:00"), //
                        hasShares(221.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Ref.-Nr.: 41638275"), //
                        hasAmount("EUR", 82.99), hasGrossValue("EUR", 97.81), //
                        hasTaxes("EUR", (0.86 / 1.0897) + 9.29 + 0.29), hasFees("EUR", 1.45 + 3.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

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
                        hasIsin("AT0000A1TW21"), hasWkn(null), hasTicker(null), //
                        hasName("RAIFF.-EMERGINGMARKETS-AKTIEN RZ(A) MITEIGENTUMSANTEILE"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-08-16T00:00"), hasExDate("2021-08-16T00:00"), //
                        hasShares(7.01), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Ref.-Nr.: 11111111"), //
                        hasAmount("EUR", 5.59), hasGrossValue("EUR", 8.34), //
                        hasTaxes("EUR", 2.15 + 0.60), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

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
                        hasIsin("DE0006599905"), hasWkn("659990"), hasTicker(null), //
                        hasName("MERCK KG A.A. INHABER-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-05-02T00:00"), hasExDate("2024-04-29T00:00"), //
                        hasShares(25.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Abrechnungsnr. 76560429680"), //
                        hasAmount("EUR", 40.50), hasGrossValue("EUR", 55.00), //
                        hasTaxes("EUR", 13.75 + 0.75), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

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
                        hasIsin("US7475251036"), hasWkn("883121"), hasTicker(null), //
                        hasName("QUALCOMM INC. REGISTERED SHARES DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-25T00:00"), hasExDate("2023-08-30T00:00"), //
                        hasShares(29.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Abrechnungsnr. 76560429680 | Quartalsdividende"), //
                        hasAmount("EUR", 16.16), hasGrossValue("EUR", 21.71), //
                        hasForexGrossValue("USD", 23.20), //
                        hasTaxes("EUR", 3.26 + 2.17 + 0.12), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12WithSecurityInEUR()
    {
        var security = new Security("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile", "EUR");
        security.setIsin("US7475251036");
        security.setWkn("883121");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

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
                        hasDate("2023-09-25T00:00"), hasExDate("2023-08-30T00:00"), //
                        hasShares(29.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Abrechnungsnr. 76560429680 | Quartalsdividende"), //
                        hasAmount("EUR", 16.16), hasGrossValue("EUR", 21.71), //
                        hasTaxes("EUR", 3.26 + 2.17 + 0.12), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende13()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0130595124"), hasWkn("13059512"), hasTicker(null), //
                        hasName("UBS ETF (CH) - SPI (R) Mid"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-09-11T00:00"), hasExDate("2024-09-06T00:00"), //
                        hasShares(254.00), //
                        hasSource("Dividende13.txt"), //
                        hasNote("Ref.-Nr.: 17518731738"), //
                        hasAmount("CHF", 392.94), hasGrossValue("CHF", 604.52), //
                        hasTaxes("CHF", 211.58), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende14()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        errors.forEach(Exception::printStackTrace);

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
                        hasIsin("AT0000821103"), hasWkn(null), hasTicker(null), //
                        hasName("UNIQA Insurance Group AG Stamm-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-06-22T00:00"), hasExDate("2026-06-18T00:00"), //
                        hasShares(261.00), //
                        hasSource("Dividende14.txt"), //
                        hasNote("Ref.-Nr.: 29438080"), //
                        hasAmount("EUR", 136.24), hasGrossValue("EUR", 187.92), //
                        hasTaxes("EUR", 51.68), hasFees("EUR", 0.00))));

    }

    @Test
    public void testDividende15()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

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
                        hasIsin("CH0012032113"), hasWkn(null), hasTicker(null), //
                        hasName("ROCHE HOLDING AG Inhaber-Aktien SF 1"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-03-16T00:00"), hasExDate("2026-03-12T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Ref.-Nr.: 81935970"), //
                        hasForexGrossValue("CHF", 980.00), //
                        hasAmount("EUR", 558.26), hasGrossValue("EUR", 1078.58), //
                        hasTaxes("EUR", 512.32), hasFees("EUR", 8.00))));
    }

    @Test
    public void testDividende15WithSecurityInEUR()
    {
        var security = new Security("ROCHE HOLDING AG Inhaber-Aktien SF 1", "EUR");
        security.setIsin("CH0012032113");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

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
                        hasDate("2026-03-16T00:00"), hasExDate("2026-03-12T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Ref.-Nr.: 81935970"), //
                        hasAmount("EUR", 558.26), hasGrossValue("EUR", 1078.58), //
                        hasTaxes("EUR", 512.32), hasFees("EUR", 8.00), //
                        check(tx -> {
                            assertThat(tx.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende16()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

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
                        hasIsin("US17275R1023"), hasWkn(null), hasTicker(null), //
                        hasName("CISCO SYSTEMS INC. Registered Shares DL-,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-01-21T00:00"), hasExDate("2026-01-02T00:00"), //
                        hasShares(500.00), //
                        hasSource("Dividende16.txt"), //
                        hasNote("Ref.-Nr.: 80052452"), //
                        hasForexGrossValue("USD", 205.00), //
                        hasAmount("EUR", 118.20), hasGrossValue("EUR", 174.07), //
                        hasTaxes("EUR", 47.87), hasFees("EUR", 8.00))));
    }

    @Test
    public void testDividende16WithSecurityInEUR()
    {
        var security = new Security("CISCO SYSTEMS INC. Registered Shares DL-,001", "EUR");
        security.setIsin("US17275R1023");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

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
                        hasDate("2026-01-21T00:00"), hasExDate("2026-01-02T00:00"), //
                        hasShares(500.00), //
                        hasSource("Dividende16.txt"), //
                        hasNote("Ref.-Nr.: 80052452"), //
                        hasAmount("EUR", 118.20), hasGrossValue("EUR", 174.07), //
                        hasTaxes("EUR", 47.87), hasFees("EUR", 8.00), //
                        check(tx -> {
                            assertThat(tx.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende17()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

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
                        hasIsin("IE00B53SZB19"), hasWkn(null), hasTicker(null), //
                        hasName("iShsVII-NASDAQ 100 UCITS ETF Reg. Shares USD (Acc) o.N."), //
                        hasCurrencyCode("USD"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2026-01-12T00:00"), hasExDate(null), //
                        hasShares(50.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Ref.-Nr.: 84111000 | Ausschüttungsgleicher Ertrag"), //
                        hasForexGrossValue("USD", 632.29), //
                        hasAmount("EUR", 540.18), hasGrossValue("EUR", 540.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17WithSecurityInEUR()
    {
        var security = new Security("iShsVII-NASDAQ 100 UCITS ETF Reg. Shares USD (Acc) o.N.", "EUR");
        security.setIsin("IE00B53SZB19");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new RaiffeisenBankgruppePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2026-01-12T00:00"), hasExDate(null), //
                        hasShares(50.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Ref.-Nr.: 84111000 | Ausschüttungsgleicher Ertrag"), //
                        hasAmount("EUR", 540.18), hasGrossValue("EUR", 540.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            assertThat(tx.getUnit(Unit.Type.GROSS_VALUE).isPresent(), is(false));

                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(17L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(17));
        new AssertImportActions().check(results, "EUR");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-01"), hasAmount("EUR", 42.13), //
                        hasSource("Kontoauszug01.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-01"), hasAmount("EUR", 10.01), //
                        hasSource("Kontoauszug01.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-01"), hasAmount("EUR", 79.58), //
                        hasSource("Kontoauszug01.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-01"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Dauerauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-02"), hasAmount("EUR", 60.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-03"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("EURO-Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-04"), hasAmount("EUR", 15.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-04"), hasAmount("EUR", 119.06), //
                        hasSource("Kontoauszug01.txt"), hasNote("EURO-Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-04"), hasAmount("EUR", 123.34), //
                        hasSource("Kontoauszug01.txt"), hasNote("EURO-Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-07"), hasAmount("EUR", 35.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("EURO-Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-18"), hasAmount("EUR", 250.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Basislastschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-12-23"), hasAmount("EUR", 85.65), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-12-28"), hasAmount("EUR", 70.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-28"), hasAmount("EUR", 34.62), //
                        hasSource("Kontoauszug01.txt"), hasNote("Basislastschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-12-29"), hasAmount("EUR", 1097.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Einnahmen"))));

        // check interest charge transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2020-12-31"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Entgelte vom 01.12.2020 - 31.12.2020"), //
                        hasAmount("EUR", 1.00 + 1.00), hasGrossValue("EUR", 1.00 + 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-12-31"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Abschluss vom 01.10.2020 bis 31.12.2020"), //
                        hasAmount("EUR", 1.95), hasGrossValue("EUR", 1.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(31L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(31));
        new AssertImportActions().check(results, "EUR");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-02"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-02"), hasAmount("EUR", 20.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-02"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-02"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-02"), hasAmount("EUR", 20.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-02"), hasAmount("EUR", 30.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-02"), hasAmount("EUR", 60.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Dauerauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-03"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-03"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-04"), hasAmount("EUR", 40.94), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-09"), hasAmount("EUR", 30.20), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-09"), hasAmount("EUR", 10.70), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-12"), hasAmount("EUR", 15.16), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-16"), hasAmount("EUR", 25.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-16"), hasAmount("EUR", 80.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Dauerauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-17"), hasAmount("EUR", 3.58), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-18"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-18"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-19"), hasAmount("EUR", 50.60), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-19"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-19"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-19"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-19"), hasAmount("EUR", 20.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-23"), hasAmount("EUR", 7.01), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-23"), hasAmount("EUR", 11.68), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-26"), hasAmount("EUR", 14.99), //
                        hasSource("Kontoauszug02.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-27"), hasAmount("EUR", 20.08), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-27"), hasAmount("EUR", 14.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-27"), hasAmount("EUR", 20.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Auszahlung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2021-08-30"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("Lohn/Gehalt"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-08-31"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Abschluss vom 30.07.2021 bis 31.08.2021"), //
                        hasAmount("EUR", 1.95), hasGrossValue("EUR", 1.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

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

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-09"), hasAmount("EUR", 136.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-15"), hasAmount("EUR", 133.29), //
                        hasSource("Kontoauszug03.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-15"), hasAmount("EUR", 139.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-16"), hasAmount("EUR", 35.19), //
                        hasSource("Kontoauszug03.txt"), hasNote("Basislastschrift"))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-08"), hasAmount("EUR", 4.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Überweisung SEPA"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-10"), hasAmount("EUR", 700.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("Überweisung SEPA"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-22"), hasAmount("EUR", 36.99), //
                        hasSource("Kontoauszug04.txt"), hasNote("Basislastschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-25"), hasAmount("EUR", 1870.70), //
                        hasSource("Kontoauszug04.txt"), hasNote("Lohn/Gehalt"))));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-20"), hasAmount("EUR", 26350.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-29"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-30"), hasAmount("EUR", 1700.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-01"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-06"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-11"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-13"), hasAmount("EUR", 700.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-14"), hasAmount("EUR", 5400.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2023-12-29"), hasAmount("EUR", 10000.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-31"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 524.91), hasGrossValue("EUR", 534.59), //
                        hasTaxes("EUR", (8.46 + 0.46 + 0.76)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug07()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(41L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(41));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-02"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-09"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-12"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-12"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-12"), hasAmount("EUR", 1111.11), //
                        hasSource("Kontoauszug07.txt"), hasNote("Lohn/Gehalt"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-15"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-16"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Retouren"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-23"), hasAmount("EUR", 10975.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-24"), hasAmount("EUR", 15.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 228.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 1167.18), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 11001.58), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-01-31"), hasAmount("EUR", 12049.12), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-02"), hasAmount("EUR", 124.40), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-02"), hasAmount("EUR", 256.21), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-03"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-04"), hasAmount("EUR", 155.42), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-05"), hasAmount("EUR", 24.09), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-05"), hasAmount("EUR", 125.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-05"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-09"), hasAmount("EUR", 141.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-12"), hasAmount("EUR", 4.99), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-12"), hasAmount("EUR", 19.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-12"), hasAmount("EUR", 291.95), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-12"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Übertrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-12"), hasAmount("EUR", 5550.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-15"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Dauerauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-16"), hasAmount("EUR", 3.53), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-19"), hasAmount("EUR", 900.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Dauerauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-24"), hasAmount("EUR", 25.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Dauerauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-24"), hasAmount("EUR", 10074.36), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-26"), hasAmount("EUR", 63.27), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-30"), hasAmount("EUR", 752.88), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-31"), hasAmount("EUR", 77.91), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-31"), hasAmount("EUR", 189.76), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-31"), hasAmount("EUR", 650.51), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-31"), hasAmount("EUR", 1516.78), //
                        hasSource("Kontoauszug07.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-31"), hasAmount("EUR", 25000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

    }

    @Test
    public void testKontoauszug08()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-24"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Ihr MastercardCashback"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-24"), hasAmount("EUR", 5.52), //
                        hasSource("Kontoauszug08.txt"), hasNote("Umsatz vom 23.04.2024 MC Hauptkarte"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-26"), hasAmount("EUR", 0.55), //
                        hasSource("Kontoauszug08.txt"), hasNote("Ihr MastercardCashback"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-22"), hasAmount("EUR", 63.39), //
                        hasSource("Kontoauszug08.txt"), hasNote("Umsatz vom 21.04.2024 MC Hauptkarte"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-29"), hasAmount("EUR", 36.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Umsatz vom 26.04.2024 MC Hauptkarte"))));

        // check interest charge transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2024-04-30"), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.11), hasGrossValue("EUR", 0.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug09()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(13L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(13));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2017-05-02"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("Dauerauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2017-05-03"), hasAmount("EUR", 3.49), //
                        hasSource("Kontoauszug09.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2017-05-10"), hasAmount("EUR", 5.99), //
                        hasSource("Kontoauszug09.txt"), hasNote("Basislastschrift"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2017-05-16"), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote("Steuerausgleich"), //
                        hasAmount("EUR", 0.27), hasGrossValue("EUR", 0.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2017-05-16"), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote("Steuerausgleich"), //
                        hasAmount("EUR", 0.44), hasGrossValue("EUR", 0.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2017-05-16"), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote("Steuerausgleich"), //
                        hasAmount("EUR", 4.93), hasGrossValue("EUR", 4.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2017-05-16"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("Basislastschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2017-05-23"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("Überweisungsgutschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2017-05-26"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("Auszahlung"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2017-05-29"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2017-05-29"), hasAmount("EUR", 8.99), //
                        hasSource("Kontoauszug09.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2017-05-29"), hasAmount("EUR", 37.74), //
                        hasSource("Kontoauszug09.txt"), hasNote("Basislastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2017-05-30"), hasAmount("EUR", 13.58), //
                        hasSource("Kontoauszug09.txt"), hasNote("Basislastschrift"))));
    }

    @Test
    public void testKontoauszug10()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2017-05-22"), hasAmount("EUR", 36072.06), //
                        hasSource("Kontoauszug10.txt"), hasNote("Überweisungsgutschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2017-05-23"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug10.txt"), hasNote("SEPA-Überweisung"))));
    }

    @Test
    public void testDepotauszug01()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-08"), hasAmount("EUR", 5000.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Gutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-17"), hasAmount("EUR", 5000.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Gutschrift"))));
    }

    @Test
    public void testDepotauszug02()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2025-09-05"), hasAmount("EUR", 200.00), //
                        hasSource("Depotauszug02.txt"), hasNote("Gutschrift"))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-09-30"), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmtausch01()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umtausch01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0392496344"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor MSCI Europe SmallCap ETF Inh.-An. I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2572257470"), hasWkn(null), hasTicker(null), //
                        hasName("AIS-MSCI Eu.SC ESG CL.NZ AMCTB Act.Nom. U.ETF EUR Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2023-03-10T00:00"), hasShares(433.00), //
                                        hasSource("Umtausch01.txt"), //
                                        hasNote("Abrechnungsnummer: 46711492"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        inboundDelivery( //
                                        hasDate("2023-03-10T00:00"), hasShares(433.00), //
                                        hasSource("Umtausch01.txt"), //
                                        hasNote("Abrechnungsnummer: 45664992 | Verhältnis: 1 : 1"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFreierErhalt01()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FreierErhalt01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKX55T58"), hasWkn(null), hasTicker(null), //
                        hasName("Vang.FTSE Develop.World U.ETF Registered Shares USD Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        inboundDelivery( //
                                        hasDate("2022-03-18T00:00"), hasShares(550.00), //
                                        hasSource("FreierErhalt01.txt"), //
                                        hasNote("Änderung/Stornierung"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFreierErhalt02()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FreierErhalt02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0038863350"), hasWkn(null), hasTicker(null), //
                        hasName("Nestlé S.A. Namens-Aktien SF -,10"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2022-07-08T00:00"), hasShares(200.00), //
                                        hasSource("FreierErhalt02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testGebuehrenbelastung01()
    {
        var extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Gebuehrenbelastung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-09-30T00:00"), //
                        hasSource("Gebuehrenbelastung01.txt"), //
                        hasNote("Ref.-Nr.: 17495598455 | 01.07.2024 bis 30.09.2024"), //
                        hasAmount("CHF", 100.04), hasGrossValue("CHF", 100.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }
}
