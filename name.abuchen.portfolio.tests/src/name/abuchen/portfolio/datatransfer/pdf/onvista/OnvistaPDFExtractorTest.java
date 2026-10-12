package name.abuchen.portfolio.datatransfer.pdf.onvista;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.feeRefund;
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
import name.abuchen.portfolio.datatransfer.pdf.OnvistaPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class OnvistaPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("DE0008490962"), hasWkn(null), hasTicker(null), //
                        hasName("DWS Deutschland Inhaber-Anteile LC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-15"), hasShares(0.7445), //
                        hasSource("Kauf01.txt"), hasNote("Abrechnungs-Nr. 65655059"), //
                        hasAmount("EUR", 150.01), hasGrossValue("EUR", 149.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("US17275R1023"), hasWkn(null), hasTicker(null), //
                        hasName("Cisco Systems Inc. Registered Shares DL-,001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-19T15:30"), hasShares(35), //
                        hasSource("Kauf02.txt"), hasNote("Abrechnungs-Nr. 59157179"), //
                        hasAmount("EUR", 1536.13), hasGrossValue("EUR", 1521.13), //
                        hasForexGrossValue("USD", 1677.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (11.03 + 5.51) / 1.1026))));
    }

    @Test
    public void testWertpapierKauf02WithSecurityInEUR()
    {
        var security = new Security("Cisco Systems Inc. Registered Shares DL-,001", "EUR");
        security.setIsin("US17275R1023");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OnvistaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

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
                        hasDate("2019-08-19T15:30"), hasShares(35), //
                        hasSource("Kauf02.txt"), hasNote("Abrechnungs-Nr. 59157179"), //
                        hasAmount("EUR", 1536.13), hasGrossValue("EUR", 1521.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (11.03 + 5.51) / 1.1026), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("DE000SE8F9E8"), hasWkn(null), hasTicker(null), //
                        hasName("Société Générale Effekten GmbH DISC.Z NVIDIA 498"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-03T15:32"), hasShares(4), //
                        hasSource("Kauf03.txt"), hasNote("Abrechnungs-Nr. 23456957"), //
                        hasAmount("EUR", 1924.52), hasGrossValue("EUR", 1922.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("DE000CBK1001"), hasWkn(null), hasTicker(null), //
                        hasName("Commerzbank AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-01-12T10:11"), hasShares(5), //
                        hasSource("Kauf04.txt"), hasNote("Abrechnungs-Nr. 27097281"), //
                        hasAmount("EUR", 59.55), hasGrossValue("EUR", 52.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.55 + 1.50))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US74767V1098"), hasWkn(null), hasTicker(null), //
                        hasName("QuantumScape Corp. Reg. Shares Cl.A  DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("VGG866591024"), hasWkn(null), hasTicker(null), //
                        hasName("Talon Metals Corp. Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-15T15:30"), hasShares(40), //
                        hasSource("Kauf05.txt"), hasNote("Abrechnungs-Nr. 28514820"), //
                        hasAmount("EUR", 1926.20), hasGrossValue("EUR", 1911.20), //
                        hasForexGrossValue("USD", 2320.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (12.14 + 6.07) / 1.2139))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-15T16:11"), hasShares(80), //
                        hasSource("Kauf05.txt"), hasNote("Abrechnungs-Nr. 52329327"), //
                        hasAmount("EUR", 3965.72), hasGrossValue("EUR", 3980.72), //
                        hasForexGrossValue("USD", 4872.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (12.24 + 6.12) / 1.2239))));

        // check tax-refund in 2rd buy sell transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2020-12-15T16:11"), //
                        hasShares(80), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Abrechnungs-Nr. 59592727"), //
                        hasAmount("EUR", 14.10), hasGrossValue("EUR", 14.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-16T12:17"), hasShares(5000), //
                        hasSource("Kauf05.txt"), hasNote("Abrechnungs-Nr. 15336433"), //
                        hasAmount("EUR", 1021.99), hasGrossValue("EUR", 1060.00), //
                        hasTaxes("EUR", 25.93 + 1.43), hasFees("EUR", 5.00 + 3.65 + 2.00))));
    }

    @Test
    public void testWertpapierKauf05WithSecurityInEUR()
    {
        var security1 = new Security("QuantumScape Corp. Reg. Shares Cl.A  DL -,0001", "EUR");
        security1.setIsin("US74767V1098");

        var client = new Client();
        client.addSecurity(security1);

        var extractor = new OnvistaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("VGG866591024"), hasWkn(null), hasTicker(null), //
                        hasName("Talon Metals Corp. Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-15T15:30"), hasShares(40), //
                        hasSource("Kauf05.txt"), hasNote("Abrechnungs-Nr. 28514820"), //
                        hasAmount("EUR", 1926.20), hasGrossValue("EUR", 1911.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (12.14 + 6.07) / 1.2139))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-15T16:11"), hasShares(80), //
                        hasSource("Kauf05.txt"), hasNote("Abrechnungs-Nr. 52329327"), //
                        hasAmount("EUR", 3965.72), hasGrossValue("EUR", 3980.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (12.24 + 6.12) / 1.2239))));

        // check tax-refund in 2rd buy sell transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2020-12-15T16:11"), //
                        hasShares(80), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Abrechnungs-Nr. 59592727"), //
                        hasAmount("EUR", 14.10), hasGrossValue("EUR", 14.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-16T12:17"), hasShares(5000), //
                        hasSource("Kauf05.txt"), hasNote("Abrechnungs-Nr. 15336433"), //
                        hasAmount("EUR", 1021.99), hasGrossValue("EUR", 1060.00), //
                        hasTaxes("EUR", 25.93 + 1.43), hasFees("EUR", 5.00 + 3.65 + 2.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("DE000A1KRCZ2"), hasWkn(null), hasTicker(null), //
                        hasName("Commerzbank AG Inhaber-Bezugsrechte"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-05-30T12:19"), hasShares(8), //
                        hasSource("Kauf06.txt"), hasNote("Abrechnungs-Nr. 17910528"), //
                        hasAmount("EUR", 6.40), hasGrossValue("EUR", 6.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

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
                        hasIsin("DE0006289473"), hasWkn(null), hasTicker(null), //
                        hasName("iS.eb.r.Go.G.1.5-2.5y U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-07-17T09:04"), hasShares(0.5638), //
                        hasSource("Kauf07.txt"), hasNote("Abrechnungs-Nr. 79899478"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax-refund buy sell transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2017-07-17T09:04"), //
                        hasShares(0.5638), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Abrechnungs-Nr. 86298863"), //
                        hasAmount("EUR", 0.06), hasGrossValue("EUR", 0.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("FR0000120271"), hasWkn(null), hasTicker(null), //
                        hasName("TotalEnergies SE Actions au Porteur EO 2,50"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-09-13T10:04"), hasShares(100), //
                        hasSource("Kauf08.txt"), hasNote(null), //
                        hasAmount("EUR", 3742.69), hasGrossValue("EUR", 3729.50), //
                        hasTaxes("EUR", 11.19), hasFees("EUR", 2.00))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("DE000TT9F809"), hasWkn(null), hasTicker(null), //
                        hasName("HSBC Trinkaus & Burkhardt AG DIZ RoyalD. 19"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-11-08T09:46"), hasShares(670), //
                        hasSource("Kauf09.txt"), hasNote("Abrechnungs-Nr. 11111111"), //
                        hasAmount("EUR", 11062.00), hasGrossValue("EUR", 11055.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 2.00))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US87165B1035"), hasWkn(null), hasTicker(null), //
                        hasName("Synchrony Financial Registered Shares DL -,001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-10-12T16:04"), hasShares(3), //
                        hasSource("Kauf10.txt"), hasNote(null), //
                        hasAmount("EUR", 90.96), hasGrossValue("EUR", 78.46), //
                        hasForexGrossValue("USD", 90.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (11.54 + 2.88) / 1.1536))));
    }

    @Test
    public void testWertpapierKauf10WithSecurityInEUR()
    {
        var security = new Security("Synchrony Financial Registered Shares DL -,001", "EUR");
        security.setIsin("US87165B1035");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OnvistaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

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
                        hasDate("2018-10-12T16:04"), hasShares(3), //
                        hasSource("Kauf10.txt"), hasNote(null), //
                        hasAmount("EUR", 90.96), hasGrossValue("EUR", 78.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (11.54 + 2.88) / 1.1536), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("DE000HB4GEE2"), hasWkn(null), hasTicker(null), //
                        hasName("11,5% UniCredit Bank AG HVB Aktienanleihe v.22(23)SDF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-12-01T18:08"), hasShares(10), //
                        hasSource("Kauf11.txt"), hasNote("Abrechnungs-Nr. 15666666 | Stückzinsaufwand EUR 82,55"), //
                        hasAmount("EUR", 991.35), hasGrossValue("EUR", 984.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 2.00))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU0603942888"), hasWkn(null), hasTicker(null), //
                        hasName("ComStage-SDAX UCITS ETF Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU0635178014"), hasWkn(null), hasTicker(null), //
                        hasName("ComSta.-MSCI Em.Mkts.TRN U.ETF Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-12-03T09:10"), hasShares(12.483), //
                        hasSource("Kauf12.txt"), hasNote("Abrechnungs-Nr. 77199603"), //
                        hasAmount("EUR", 600.00), hasGrossValue("EUR", 600.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-12-03T09:06"), hasShares(8.1741), //
                        hasSource("Kauf12.txt"), hasNote("Abrechnungs-Nr. 55390681"), //
                        hasAmount("EUR", 800.00), hasGrossValue("EUR", 800.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-12-03T09:06"), hasShares(20.2778), //
                        hasSource("Kauf12.txt"), hasNote("Abrechnungs-Nr. 13722388"), //
                        hasAmount("EUR", 800.00), hasGrossValue("EUR", 800.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf13.txt"), errors);

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
                        hasIsin("DE0008404005"), hasWkn(null), hasTicker(null), //
                        hasName("Allianz SE vink.Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-02-03T16:04"), hasShares(7.00), //
                        hasSource("Kauf13.txt"), hasNote("Abrechnungs-Nr. 93213477"), //
                        hasAmount("EUR", 987.02), hasGrossValue("EUR", 985.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.50))));
    }

    @Test
    public void testWertpapierKauf14()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf14.txt"), errors);

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
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-10-15T10:10"), hasShares(2.1524), //
                        hasSource("Kauf14.txt"), hasNote("Abrechnungs-Nr. 63759300"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0Z23Q5"), hasWkn(null), hasTicker(null), //
                        hasName("adesso AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-09-02T09:10"), hasShares(20), //
                        hasSource("Verkauf01.txt"), hasNote("Abrechnungs-Nr. 12345678"), //
                        hasAmount("EUR", 623.49), hasGrossValue("EUR", 630.00), //
                        hasTaxes("EUR", 1.43 + 0.08), hasFees("EUR", 5.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-09-02T09:10"), hasShares(80), //
                        hasSource("Verkauf01.txt"), hasNote("Abrechnungs-Nr. 1234567"), //
                        hasAmount("EUR", 2508.47), hasGrossValue("EUR", 2520.00), //
                        hasTaxes("EUR", 9.51 + 0.52), hasFees("EUR", 1.50))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0L1H32"), hasWkn(null), hasTicker(null), //
                        hasName("MPH Mittelst.Pharma Hldg AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005558696"), hasWkn(null), hasTicker(null), //
                        hasName("paragon AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-09-14T09:02"), hasShares(14), //
                        hasSource("Verkauf02.txt"), hasNote("Abrechnungs-Nr. 12345678"), //
                        hasAmount("EUR", 32.70), hasGrossValue("EUR", 39.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 1.50))));

        // check tax-refund in 1st buy sell transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2016-09-14T09:02"), //
                        hasShares(14), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Abrechnungs-Nr. 47883712"), //
                        hasAmount("EUR", 1.18), hasGrossValue("EUR", 1.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-09-14T12:54"), hasShares(55), //
                        hasSource("Verkauf02.txt"), hasNote("Abrechnungs-Nr. 12345678"), //
                        hasAmount("EUR", 1665.41), hasGrossValue("EUR", 1676.95), //
                        hasTaxes("EUR", 4.78 + 0.26), hasFees("EUR", 5.00 + 1.50))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

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
                        hasIsin("DE000A1KRRB1"), hasWkn(null), hasTicker(null), //
                        hasName("Porsche Automobil Holding SE Inhaber-Bezugsrechte auf VZO"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2011-04-08T12:30"), hasShares(4), //
                        hasSource("Verkauf03.txt"), hasNote("Abrechnungs-Nr. 78345409"), //
                        hasAmount("EUR", 21.45), hasGrossValue("EUR", 22.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.75))));

        // check tax-refund in 1st buy sell transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2011-04-08T12:30"), //
                        hasShares(4), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Abrechnungs-Nr. 30158878"), //
                        hasAmount("EUR", 0.28), hasGrossValue("EUR", 0.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("DE000A1KRRB1"), hasWkn(null), hasTicker(null), //
                        hasName("Porsche Automobil Holding SE Inhaber-Bezugsrechte auf VZO"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-01-21T16:34"), hasShares(4), //
                        hasSource("Verkauf04.txt"), hasNote(null), //
                        hasAmount("EUR", 21.45), hasGrossValue("EUR", 22.20), //
                        hasTaxes("EUR", 0.25 + 0.05), hasFees("EUR", 0.20 + 0.25))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

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
                        hasIsin("DE000CBKTLR7"), hasWkn(null), hasTicker(null), //
                        hasName("Commerzbank AG Inhaber-Teilrechte"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2013-05-06T12:00"), hasShares(0.5), //
                        hasSource("Verkauf05.txt"), hasNote("Abrechnungs-Nr. 66867433"), //
                        hasAmount("EUR", 5.41), hasGrossValue("EUR", 5.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax-refund in 1st buy sell transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2013-05-06T12:00"), //
                        hasShares(0.5), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Abrechnungs-Nr. 56072633"), //
                        hasAmount("EUR", 3.05), hasGrossValue("EUR", 3.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

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
                        hasIsin("DE000MC55366"), hasWkn(null), hasTicker(null), //
                        hasName("Morgan Stanley & Co. Intl PLC DIZ Fres. SE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-25"), hasShares(65), //
                        hasSource("Verkauf06.txt"), hasNote("Abrechnungs-Nr. 10283354"), //
                        hasAmount("EUR", 2563.60), hasGrossValue("EUR", 2563.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax-refund in buy sell transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2020-09-25"), //
                        hasShares(65), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("Abrechnungs-Nr. 18633554"), //
                        hasAmount("EUR", 74.63), hasGrossValue("EUR", 74.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf07()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

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
                        hasIsin("DE000TUAG117"), hasWkn(null), hasTicker(null), //
                        hasName("TUI AG Wandelanl.v.2009(2014)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2014-11-17"), hasShares(1), //
                        hasSource("Verkauf07.txt"), hasNote("Abrechnungs-Nr. 25720768"), //
                        hasAmount("EUR", 51.85), hasGrossValue("EUR", 56.30), //
                        hasTaxes("EUR", 4.22 + 0.23), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf08()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000GD8RHG7"), hasWkn(null), hasTicker(null), //
                        hasName("Goldman Sachs Wertpapier GmbH TuBear 24.11.17 DAX 13410"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000LS27QS5"), hasWkn(null), hasTicker(null), //
                        hasName("Lang & Schwarz AG Turbo 31.01.18 DAX"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000PP09P16"), hasWkn(null), hasTicker(null), //
                        hasName("BNP Paribas Em.-u.Handelsg.mbH TurboL 30.11.17 S&P500 2550"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000PP1BEF0"), hasWkn(null), hasTicker(null), //
                        hasName("BNP Paribas Em.-u.Handelsg.mbH TurboL 18.12.17 DAX 13170"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010959676"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi ETF MSCI Emerging Mkts Actions au Porteur o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-11-10T09:56"), hasShares(200), //
                        hasSource("Verkauf08.txt"), hasNote(null), //
                        hasAmount("EUR", 482.66), hasGrossValue("EUR", 524.60), //
                        hasTaxes("EUR", 31.22 + 1.72 + 2.50), hasFees("EUR", 5.00 + 1.50))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-10T10:54"), hasShares(200), //
                        hasSource("Verkauf08.txt"), hasNote(null), //
                        hasAmount("EUR", 424.50), hasGrossValue("EUR", 418.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 1.50))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-11-09T12:55"), hasShares(1000), //
                        hasSource("Verkauf08.txt"), hasNote(null), //
                        hasAmount("EUR", 303.50), hasGrossValue("EUR", 310.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 1.50))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-11-09T12:56"), hasShares(100), //
                        hasSource("Verkauf08.txt"), hasNote(null), //
                        hasAmount("EUR", 139.50), hasGrossValue("EUR", 146.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 1.50))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-11-09T13:43"), hasShares(700), //
                        hasSource("Verkauf08.txt"), hasNote(null), //
                        hasAmount("EUR", 2946.50), hasGrossValue("EUR", 2940.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 1.50))));
    }

    @Test
    public void testWertpapierVerkauf09()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ZAL1111"), hasWkn(null), hasTicker(null), //
                        hasName("Zalando SE Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0006632003"), hasWkn(null), hasTicker(null), //
                        hasName("MorphoSys AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-08-08T15:00"), hasShares(100), //
                        hasSource("Verkauf09.txt"), hasNote("Abrechnungs-Nr. 98765432"), //
                        hasAmount("EUR", 4361.00), hasGrossValue("EUR", 4354.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 1.50))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-08-08T17:27"), hasShares(100), //
                        hasSource("Verkauf09.txt"), hasNote("Abrechnungs-Nr. 68411850"), //
                        hasAmount("EUR", 4293.60), hasGrossValue("EUR", 4300.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 1.50))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-08-08T17:28"), hasShares(40), //
                        hasSource("Verkauf09.txt"), hasNote("Abrechnungs-Nr. 80926283"), //
                        hasAmount("EUR", 4234.10), hasGrossValue("EUR", 4227.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 1.50))));

        // check tax-refund buy sell transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2018-08-08T17:27"), //
                        hasShares(100), //
                        hasSource("Verkauf09.txt"), //
                        hasNote("Abrechnungs-Nr. 80817950"), //
                        hasAmount("EUR", 17.78), hasGrossValue("EUR", 17.78), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf10()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf10.txt"), errors);

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
                        hasIsin("US83416M1053"), hasWkn(null), hasTicker(null), //
                        hasName("SLR Senior Investment Corp. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-12-14T18:55"), hasShares(100), //
                        hasSource("Verkauf10.txt"), hasNote("Abrechnungs-Nr. 60770481"), //
                        hasAmount("EUR", 1259.14), hasGrossValue("EUR", 1264.14), //
                        hasForexGrossValue("USD", 1430.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.66 / 1.1312))));
    }

    @Test
    public void testWertpapierVerkauf10WithSecurityInEUR()
    {
        var security = new Security("SLR Senior Investment Corp. Registered Shares DL -,01", "EUR");
        security.setIsin("US83416M1053");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OnvistaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf10.txt"), errors);

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
        assertThat(results, hasItem(sale( //
                        hasDate("2021-12-14T18:55"), hasShares(100), //
                        hasSource("Verkauf10.txt"), hasNote("Abrechnungs-Nr. 60770481"), //
                        hasAmount("EUR", 1259.14), hasGrossValue("EUR", 1430.00 / 1.1312), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.66 / 1.1312), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf11()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf11.txt"), errors);

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
                        hasIsin("DE000UH42Q17"), hasWkn(null), hasTicker(null), //
                        hasName("UBS AG (London Branch) TurboP O.End VW Vz 171,844309"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-12-14"), hasShares(250), //
                        hasSource("Verkauf11.txt"), hasNote("Abrechnungs-Nr. 42189222"), //
                        hasAmount("EUR", 0.25), hasGrossValue("EUR", 0.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax-refund in buy sell transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-12-14"), //
                        hasShares(250), //
                        hasSource("Verkauf11.txt"), //
                        hasNote("Abrechnungs-Nr. 54606022"), //
                        hasAmount("EUR", 43.32), hasGrossValue("EUR", 43.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf12()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf12.txt"), errors);

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
                        hasIsin("GB0059822006"), hasWkn(null), hasTicker(null), //
                        hasName("Dialog Semiconductor PLC Registered Shares LS -,10"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-09-09T00:00"), hasShares(66.00), //
                        hasSource("Verkauf12.txt"), //
                        hasNote("Abrechnungs-Nr. 11111111"), //
                        hasAmount("EUR", 3509.20), hasGrossValue("EUR", 4455.00), //
                        hasTaxes("EUR", 896.49 + 49.31), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf13()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf13.txt"), errors);

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
                        hasIsin("DE0001102358"), hasWkn(null), hasTicker(null), //
                        hasName("Bundesrep.Deutschland Anl.v.2014 (2024)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-15T00:00"), hasShares(200.00), //
                        hasSource("Verkauf13.txt"), //
                        hasNote("Abrechnungs-Nr. 33333333"), //
                        hasAmount("EUR", 20000.00), hasGrossValue("EUR", 20000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("DE000CBK1001"), hasWkn(null), hasTicker(null), //
                        hasName("Commerzbank AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-04-21"), hasExDate("2016-04-21"), //
                        hasShares(50), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abrechnungs-Nr. 77110599"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010296061"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor ETF MSCI USA Actions au Porteur D-EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010315770"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor ETF MSCI WORLD FCP Actions au Port.D-EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0496786574"), hasWkn(null), hasTicker(null), //
                        hasName("MUL-LYXOR S&P 500 UCITS ETF Inhaber-Anteile D-EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-12-16"), hasExDate("2016-12-14"), //
                        hasShares(1.0545), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungs-Nr. 55746925 | Ertrag für 2016/17"), //
                        hasAmount("EUR", 1.80), hasGrossValue("EUR", 1.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-12-16"), hasExDate("2016-12-14"), //
                        hasShares(1.2879), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungs-Nr. 97603916 | Ertrag für 2016/17"), //
                        hasAmount("EUR", 1.80), hasGrossValue("EUR", 1.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-12-16"), hasExDate("2016-12-14"), //
                        hasShares(9.9225), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungs-Nr. 33071326 | Ertrag für 2016"), //
                        hasAmount("EUR", 1.89), hasGrossValue("EUR", 1.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("US56035L1044"), hasWkn(null), hasTicker(null), //
                        hasName("Main Street Capital Corp. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-07-15"), hasExDate("2019-06-27"), //
                        hasShares(100), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.47), hasGrossValue("EUR", 18.10), //
                        hasForexGrossValue("USD", 18.10 * 1.1327), //
                        hasTaxes("EUR", 2.72 + 1.81 + 0.10), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("Main Street Capital Corp. Registered Shares DL -,01", "EUR");
        security.setIsin("US56035L1044");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OnvistaPDFExtractor(client);

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
                        hasDate("2019-07-15"), hasExDate("2019-06-27"), //
                        hasShares(100), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.47), hasGrossValue("EUR", 18.10), //
                        hasTaxes("EUR", 2.72 + 1.81 + 0.10), hasFees("EUR", 0.00), //
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
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("DK0060534915"), hasWkn(null), hasTicker(null), //
                        hasName("Novo-Nordisk AS Navne-Aktier B DK -,20"), //
                        hasCurrencyCode("DKK"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-03-26"), hasExDate("2019-03-22"), //
                        hasShares(6), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungs-Nr. 29013705"), //
                        hasAmount("EUR", 3.02), hasGrossValue("EUR", 4.13), //
                        hasForexGrossValue("DKK", 4.13 * 7.483), //
                        hasTaxes("EUR", 8.34 / 7.483), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("Novo-Nordisk AS Navne-Aktier B DK -,20", "EUR");
        security.setIsin("DK0060534915");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OnvistaPDFExtractor(client);

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
                        hasDate("2019-03-26"), hasExDate("2019-03-22"), //
                        hasShares(6), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungs-Nr. 29013705"), //
                        hasAmount("EUR", 3.02), hasGrossValue("EUR", 4.13), //
                        hasTaxes("EUR", 8.34 / 7.483), hasFees("EUR", 0.00), //
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
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("XS0162869076"), hasWkn(null), hasTicker(null), //
                        hasName("5,875% Telefónica Europe B.V. EO-Medium-Term Notes 2003(33)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-02-14"), hasExDate(null), //
                        hasShares(50), //
                        hasSource("Dividende05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 219.17), hasGrossValue("EUR", 293.75), //
                        hasTaxes("EUR", 65.14 + 3.58 + 5.86), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("LU0140355917"), hasWkn(null), hasTicker(null), //
                        hasName("Allianz Euro Bond Fund Inhaber-Anteile A (EUR) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-03-04"), hasExDate("2015-03-02"), //
                        hasShares(28), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Abrechnungs-Nr. 96937413 | Ertrag für 2014"), //
                        hasAmount("EUR", 21.69), hasGrossValue("EUR", 30.65), //
                        hasTaxes("EUR", 7.83 + 0.43 + 0.70), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("DE0002635307"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.STOXX Europe 600 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-12-15"), hasExDate("2016-12-15"), //
                        hasShares(5.8192), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Abrechnungs-Nr. 14053767 | Ertrag für 2016/17"), //
                        hasAmount("EUR", 1.16), hasGrossValue("EUR", 1.19), //
                        hasTaxes("EUR", 0.03), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("LU0340285161"), hasWkn(null), hasTicker(null), //
                        hasName("UBS-ETF-UBS-ETF MSCI Wld U.ETF Inhaber-Anteile (USD) A-dis oN"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-02-05"), hasExDate("2019-01-31"), //
                        hasShares(32), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Abrechnungs-Nr. 12345 | Ertrag für 2018"), //
                        hasAmount("EUR", 39.60), hasGrossValue("EUR", 39.60), //
                        hasForexGrossValue("USD", 45.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08WithSecurityInEUR()
    {
        var security = new Security("UBS-ETF-UBS-ETF MSCI Wld U.ETF Inhaber-Anteile (USD) A-dis oN", "EUR");
        security.setIsin("LU0340285161");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OnvistaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

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
                        hasDate("2019-02-05"), hasExDate("2019-01-31"), //
                        hasShares(32), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Abrechnungs-Nr. 12345 | Ertrag für 2018"), //
                        hasAmount("EUR", 39.60), hasGrossValue("EUR", 39.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("NL0000388619"), hasWkn(null), hasTicker(null), //
                        hasName("Unilever N.V. Aandelen op naam EO -,16"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-09-11"), hasExDate("2019-08-08"), //
                        hasShares(40), //
                        hasSource("Dividende09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.23), hasGrossValue("EUR", 16.42), //
                        hasTaxes("EUR", 2.46 + 1.64 + 0.09), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("JP3165650007"), hasWkn(null), hasTicker(null), //
                        hasName("NTT DOCOMO INC. Registered Shares o.N."), //
                        hasCurrencyCode("JPY"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-25"), hasExDate("2020-09-29"), //
                        hasShares(46), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Abrechnungs-Nr. 11223344"), //
                        hasAmount("EUR", 16.91), hasGrossValue("EUR", 22.80), //
                        hasForexGrossValue("JPY", 22.80 * 121.06), //
                        hasTaxes("EUR", ((423 / 121.06) + 2.28 + 0.12)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10WithSecurityInEUR()
    {
        var security = new Security("NTT DOCOMO INC. Registered Shares o.N.", "EUR");
        security.setIsin("JP3165650007");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OnvistaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

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
                        hasDate("2020-11-25"), hasExDate("2020-09-29"), //
                        hasShares(46), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Abrechnungs-Nr. 11223344"), //
                        hasAmount("EUR", 16.91), hasGrossValue("EUR", 22.80), //
                        hasTaxes("EUR", ((423 / 121.06) + 2.28 + 0.12)), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende11()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("US37950E5490"), hasWkn(null), hasTicker(null), //
                        hasName("Global X SuperDividend ETF Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-12"), hasExDate("2020-11-04"), //
                        hasShares(500), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Abrechnungs-Nr. 23344420 | Ertrag für 2020"), //
                        hasAmount("EUR", 23.53), hasGrossValue("EUR", 31.61), //
                        hasForexGrossValue("USD", 37.50), //
                        hasTaxes("EUR", 4.74 + 3.16 + 0.18), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11WithSecurityInEUR()
    {
        var security = new Security("Global X SuperDividend ETF Registered Shares o.N.", "EUR");
        security.setIsin("US37950E5490");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OnvistaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

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
                        hasDate("2020-11-12"), hasExDate("2020-11-04"), //
                        hasShares(500), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Abrechnungs-Nr. 23344420 | Ertrag für 2020"), //
                        hasAmount("EUR", 23.53), hasGrossValue("EUR", 31.61), //
                        hasTaxes("EUR", 4.74 + 3.16 + 0.18), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende12()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("DE000TUAG117"), hasWkn(null), hasTicker(null), //
                        hasName("5,5% TUI AG Wandelanl.v.2009(2014)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-11-17"), hasExDate(null), //
                        hasShares(1), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Abrechnungs-Nr. 63302459"), //
                        hasAmount("EUR", 1.14), hasGrossValue("EUR", 1.55), //
                        hasTaxes("EUR", 0.39 + 0.02), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende13()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0002635307"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.STOXX Europe 600 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-09-15"), hasExDate("2021-09-15"), //
                        hasShares(549), //
                        hasSource("Dividende13.txt"), //
                        hasNote("Abrechnungs-Nr. 34091609 | Ertrag für 2021/22"), //
                        hasAmount("EUR", 113.75), hasGrossValue("EUR", 141.49), //
                        hasTaxes("EUR", 24.22 + 1.34 + 1.09 + 1.09), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

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
                        hasIsin("US3165001070"), hasWkn(null), hasTicker(null), //
                        hasName("Fidus Investment Corp. Registered Shares DL -,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-12-17"), hasExDate("2021-12-02"), //
                        hasShares(1000), //
                        hasSource("Dividende14.txt"), //
                        hasNote("Abrechnungs-Nr. 59788848"), //
                        hasAmount("EUR", 269.63), hasGrossValue("EUR", 362.16), //
                        hasForexGrossValue("USD", 362.16 * 1.1321), //
                        hasTaxes("EUR", 54.32 + 36.22 + 1.99), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14WithSecurityInEUR()
    {
        var security = new Security("Fidus Investment Corp. Registered Shares DL -,001", "EUR");
        security.setIsin("US3165001070");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OnvistaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

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
                        hasDate("2021-12-17"), hasExDate("2021-12-02"), //
                        hasShares(1000), //
                        hasSource("Dividende14.txt"), //
                        hasNote("Abrechnungs-Nr. 59788848"), //
                        hasAmount("EUR", 269.63), hasGrossValue("EUR", 362.16), //
                        hasTaxes("EUR", 54.32 + 36.22 + 1.99), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende15()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("DE000A0H08A8"), hasWkn(null), hasTicker(null), //
                        hasName("iS.EO Go.B.C.2.5-5.5y.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2017-10-20"), hasExDate("2017-10-06"), //
                        hasShares(0.4512), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Abrechnungs-Nr. 12345678 | Ertrag für 2017"), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende16()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("CH0114405324"), hasWkn(null), hasTicker(null), //
                        hasName("Garmin Ltd. Namens-Aktien SF 0,10"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-30"), hasExDate(null), //
                        hasShares(15), //
                        hasSource("Dividende16.txt"), //
                        hasNote("Abrechnungs-Nr. 42739637 | Kapitalrückzahlung"), //
                        hasAmount("EUR", 8.21), hasGrossValue("EUR", 11.16), //
                        hasForexGrossValue("USD", 10.95), //
                        hasTaxes("EUR", 2.95), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende16WithSecurityInEUR()
    {
        var security = new Security("Garmin Ltd. Namens-Aktien SF 0,10", "EUR");
        security.setIsin("CH0114405324");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OnvistaPDFExtractor(client);

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
                        hasDate("2022-09-30"), hasExDate(null), //
                        hasShares(15), //
                        hasSource("Dividende16.txt"), //
                        hasNote("Abrechnungs-Nr. 42739637 | Kapitalrückzahlung"), //
                        hasAmount("EUR", 8.21), hasGrossValue("EUR", 11.16), //
                        hasTaxes("EUR", 2.95), hasFees("EUR", 0.00), //
                        check(tx -> {
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
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("NO0003054108"), hasWkn(null), hasTicker(null), //
                        hasName("Mowi ASA Navne-Aksjer NK 7,50"), //
                        hasCurrencyCode("NOK"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-09-09"), hasExDate("2019-08-30"), //
                        hasShares(60.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Abrechnungs-Nr. 26128781"), //
                        hasAmount("EUR", 7.65), hasGrossValue("EUR", 15.73), //
                        hasForexGrossValue("NOK", 155.96), //
                        hasTaxes("EUR", (39.00 / 9.9148) + 3.93 + 0.22), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17WithSecurityInEUR()
    {
        var security = new Security("Mowi ASA Navne-Aksjer NK 7,50", "EUR");
        security.setIsin("NO0003054108");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OnvistaPDFExtractor(client);

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

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-09-09"), hasExDate("2019-08-30"), //
                        hasShares(60.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Abrechnungs-Nr. 26128781"), //
                        hasAmount("EUR", 7.65), hasGrossValue("EUR", 15.73), //
                        hasTaxes("EUR", (39.00 / 9.9148) + 3.93 + 0.22), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende18()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende18.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0165915215"), hasWkn(null), hasTicker(null), //
                        hasName("AGIF-Allianz Euro Bond Inhaber Anteile A (EUR) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-12-17T00:00"), hasExDate("2015-12-15"), //
                        hasShares(156.729), //
                        hasSource("Dividende18.txt"), //
                        hasNote("Abrechnungs-Nr. 70187215 | Ertrag für 2014/15"), //
                        hasAmount("EUR", 7.68), hasGrossValue("EUR", 11.84), //
                        hasTaxes("EUR", 4.16), hasFees("EUR", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2015-12-17T00:00"), hasExDate("2015-12-15"), //
                                        hasShares(156.729), //
                                        hasSource("Dividende18.txt"), //
                                        hasNote("Ausführungs-Nr. 33217061"), //
                                        hasAmount("EUR", 3.01), hasGrossValue("EUR", 3.01), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividendeStorno01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno01.txt"), errors);

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
                        hasIsin("US26884U1097"), hasWkn(null), hasTicker(null), //
                        hasName("EPR Properties Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2020-05-15"), hasExDate("2020-04-29"), //
                                        hasShares(46), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote("Abrechnungs-Nr. 31510000 | Storno unserer Dividendengutschrift Nr. 67390000 vom 15.05.2020."), //
                                        hasAmount("EUR", 13.73), hasGrossValue("EUR", 18.70), //
                                        hasTaxes("EUR", 2.42 + 2.42 + 0.13), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividendeWithReinvest01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeWithReinvest01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A1TNRX5"), hasWkn(null), hasTicker(null), //
                        hasName("Deutsche Telekom AG Dividend in Kind-Cash Line"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005557508"), hasWkn(null), hasTicker(null), //
                        hasName("Deutsche Telekom AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2013-05-17"), hasExDate("2013-05-17"), //
                        hasShares(25), //
                        hasSource("DividendeWithReinvest01.txt"), //
                        hasNote("Abrechnungs-Nr. 17299829"), //
                        hasAmount("EUR", 17.50), hasGrossValue("EUR", 17.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2013-05-17"), hasShares(25), //
                        hasSource("DividendeWithReinvest01.txt"), //
                        hasNote("Abrechnungs-Nr. 17299829 | Reinvestierung: DE0005557508"), //
                        hasAmount("EUR", 17.50), hasGrossValue("EUR", 17.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividendeWithReinvest02()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeWithReinvest02.txt"), errors);

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
                        hasIsin("GB0007908733"), hasWkn(null), hasTicker(null), //
                        hasName("SSE PLC Shs LS-,50"), //
                        hasCurrencyCode("GBP"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-03-13"), hasExDate("2020-01-16"), hasShares(75), //
                        hasSource("DividendeWithReinvest02.txt"), hasNote("Abrechnungs-Nr. 12345678"), //
                        hasAmount("EUR", 20.39), hasGrossValue("EUR", 20.39), hasForexGrossValue("GBP", 18.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-13"), hasShares(1), //
                        hasSource("DividendeWithReinvest02.txt"),
                        hasNote("Abrechnungs-Nr. 12345678 | Reinvestierung: GB0007908733"), //
                        hasAmount("EUR", 26.89), hasGrossValue("EUR", 16.89), hasForexGrossValue("GBP", 14.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (8.83 / 0.88295)))));
    }

    @Test
    public void testDividendeWithReinvest02WithSecurityInEUR()
    {
        var security = new Security("SSE PLC Shs LS-,50", "EUR");
        security.setIsin("GB0007908733");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OnvistaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeWithReinvest02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-03-13"), hasExDate("2020-01-16"), hasShares(75), //
                        hasSource("DividendeWithReinvest02.txt"), hasNote("Abrechnungs-Nr. 12345678"), //
                        hasAmount("EUR", 20.39), hasGrossValue("EUR", 20.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-13"), hasShares(1), //
                        hasSource("DividendeWithReinvest02.txt"),
                        hasNote("Abrechnungs-Nr. 12345678 | Reinvestierung: GB0007908733"), //
                        hasAmount("EUR", 26.89), hasGrossValue("EUR", 16.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (8.83 / 0.88295)), check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividendeWithOutbondDelivery()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeWithOutbondDelivery01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A3MQQ33"), hasWkn(null), hasTicker(null), //
                        hasName("Vonovia SE Dividende Cash"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-25T00:00"), hasExDate("2022-05-23"), hasShares(6.00), //
                        hasSource("DividendeWithOutbondDelivery01.txt"), //
                        hasNote("Abrechnungs-Nr. 12134880"), //
                        hasAmount("EUR", 9.96), hasGrossValue("EUR", 9.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2022-05-25T00:00"), hasShares(6.00), //
                                        hasSource("DividendeWithOutbondDelivery01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ETFL011"), hasWkn(null), hasTicker(null), //
                        hasName("Deka DAX UCITS ETF Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005933923"), hasWkn(null), hasTicker(null), //
                        hasName("iShares MDAX UCITS ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check 1st tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-02"), //
                        hasShares(0.4298), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote("Abrechnungs-Nr. 21408694"), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-02"), //
                        hasShares(0.9265), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote("Abrechnungs-Nr. 96003514"), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(2L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKM4GZ66"), hasWkn(null), hasTicker(null), //
                        hasName("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU1834988518"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor IF-L.ST.Eur.600 Technol. Act. au Port. EUR Acc. oN"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation (Amount = 0,00) transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        taxes( //
                                        hasDate("2020-01-02"), hasShares(100.00), //
                                        hasSource("Vorabpauschale02.txt"), //
                                        hasNote("Ausführungs-Nr. 82128903"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check cancellation (Amount = 0,00) transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        taxes( //
                                        hasDate("2020-01-02"), hasShares(100.00), //
                                        hasSource("Vorabpauschale02.txt"), //
                                        hasNote("Ausführungs-Nr. 14381407"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testVorabpauschale03()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(2L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BTJRMP35"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE)-MSCI Emerging Markets Reg. Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU0908508814"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.II Gbl Infl.-Linked Bond Inhaber-Anteile 5C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check cancellation (Amount = 0,00) transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        taxes( //
                                        hasDate("2021-01-04"), hasShares(0.1), //
                                        hasSource("Vorabpauschale03.txt"), //
                                        hasNote("Ausführungs-Nr. 66023908"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check cancellation (Amount = 0,00) transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        taxes( //
                                        hasDate("2021-01-04"), hasShares(0.1), //
                                        hasSource("Vorabpauschale03.txt"), //
                                        hasNote("Ausführungs-Nr. 55108371"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testVorabpauschale04()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check cancellation (Amount = 0,00) transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        taxes( //
                                        hasDate("2020-01-02"), hasShares(171.6149), //
                                        hasSource("Vorabpauschale04.txt"), //
                                        hasNote("Ausführungs-Nr. 66023908"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKapitalerhoehung01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kapitalerhoehung01.txt"), errors);

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
                        hasIsin("DE0008032004"), hasWkn(null), hasTicker(null), //
                        hasName("Commerzbank AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2011-04-06T00:00"), hasShares(25.00), //
                                        hasSource("Kapitalerhoehung01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKapitalherabsetzung01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kapitalherabsetzung01.txt"), errors);
        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008032004"), hasWkn(null), hasTicker(null), //
                        hasName("Commerzbank AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000CBKTLR7"), hasWkn(null), hasTicker(null), //
                        hasName("Commerzbank AG Inhaber-Teilrechte"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        outboundDelivery( //
                                        hasDate("2013-04-24T00:00"), hasShares(55.00), //
                                        hasSource("Kapitalherabsetzung01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2013-04-24T00:00"), hasShares(5.00), //
                                        hasSource("Kapitalherabsetzung01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKapitalherabsetzung02()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kapitalherabsetzung02.txt"), errors);
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
                        hasIsin("DE000TUAG000"), hasWkn(null), hasTicker(null), //
                        hasName("TUI AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2023-02-24T00:00"), hasShares(50.00), //
                                        hasSource("Kapitalherabsetzung02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testSplit01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Split01.txt"), errors);
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
                        hasIsin("SG1N28909355"), hasWkn(null), hasTicker(null), //
                        hasName("Ocean Sky International Ltd Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        outboundDelivery( //
                                        hasDate("2016-11-25T00:00"), hasShares(2000.00), //
                                        hasSource("Split01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertloseAusbuchung01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "WertloseAusbuchung01.txt"), errors);

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
                        hasIsin("DE000A2AA2C3"), hasWkn(null), hasTicker(null), //
                        hasName("Deutsche Telekom AG Dividend in Kind-Cash Line"), //
                        hasCurrencyCode("EUR"))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2016-06-21"), hasShares(25), //
                        hasSource("WertloseAusbuchung01.txt"), hasNote(null), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFreieLieferung01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "FreieLieferung01.txt"), errors);

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
                        hasIsin("LU1931975079"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi I.S.-Am.EUR Corp.Bond Nam.-Ant.UC.ETF DR EUR Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2024-02-07T00:00"), hasShares(50.00), //
                                        hasSource("FreieLieferung01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testEinbuchungVonRechten01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EinbuchungVonRechten01.txt"), errors);

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
                        hasIsin("DE0005557508"), hasWkn(null), hasTicker(null), //
                        hasName("Deutsche Telekom AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2016-05-25T00:00"), hasShares(25.00), //
                                        hasSource("EinbuchungVonRechten01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFusion01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fusion01.txt"), errors);

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
                        hasIsin("LU0269583422"), hasWkn(null), hasTicker(null), //
                        hasName("Gagfah S.A. Actions nom. EO 1,25"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2017-07-04T00:00"), hasShares(12.00), //
                                        hasSource("Fusion01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividendeWithCashCompensation01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeWithCashCompensation01.txt"),
                        errors);

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
                        hasIsin("DE000A1TNRX5"), hasWkn(null), hasTicker(null), //
                        hasName("Deutsche Telekom AG Dividend in Kind-Cash Line"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2013-06-11"), hasShares(25), //
                        hasSource("DividendeWithCashCompensation01.txt"), hasNote("Abrechnungs-Nr. 60738913"), //
                        hasAmount("EUR", 17.50), hasGrossValue("EUR", 17.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testRegistrierungsgebuehr01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Registrierungsgebuehr01.txt"), errors);

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
                        hasIsin("DE000A1ML7J1"), hasWkn(null), hasTicker(null), //
                        hasName("Vonovia SE Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2017-07-24"), //
                        hasShares(6), //
                        hasSource("Registrierungsgebuehr01.txt"), //
                        hasNote("Abrechnungs-Nr. 63550522 | Registrierung der Namens-Aktien"), //
                        hasAmount("EUR", 0.89), hasGrossValue("EUR", 0.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testZwangsabfindung01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zwangsabfindung01.txt"), errors);

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
                        hasIsin("DE000SKYD000"), hasWkn(null), hasTicker(null), //
                        hasName("Sky Deutschland AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-09-22"), hasShares(25), //
                        hasSource("Zwangsabfindung01.txt"), //
                        hasNote("Abrechnungs-Nr. 79573870 | Zwangsabfindung gemäß Hauptversammlungsbeschluss vom 22. Juli 2015."), //
                        hasAmount("EUR", 167.00), hasGrossValue("EUR", 167.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmtausch01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umtausch01.txt"), errors);

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
                        hasIsin("DE000A1KRCZ2"), hasWkn(null), hasTicker(null), //
                        hasName("Commerzbank AG Inhaber-Bezugsrechte"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2011-06-06T00:00"), hasShares(33.00), //
                                        hasSource("Umtausch01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmtausch02()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umtausch02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0165915215"), hasWkn(null), hasTicker(null), //
                        hasName("AGIF-Allianz Euro Bond Inhaber Anteile A (EUR) o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU0140355917"), hasWkn(null), hasTicker(null), //
                        hasName("Allianz Euro Bond Fund Inhaber-Anteile A (EUR) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2015-11-26T00:00"), hasShares(156.729), //
                                        hasSource("Umtausch02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2015-11-23T00:00"), hasShares(28.00), //
                                        hasSource("Umtausch02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmtausch03()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umtausch03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1900068328"), hasWkn(null), hasTicker(null), //
                        hasName("MUL-Lyx.MSCI AC As.Paci.e.Jap. Act. au Port. EUR Acc. oN"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0010312124"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor MSCI AC As.Pa.x Ja.U.ETF Act. au Port. Acc o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2019-02-26T00:00"), hasShares(1.9315), //
                                        hasSource("Umtausch03.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2019-02-22T00:00"), hasShares(1.9315), //
                                        hasSource("Umtausch03.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmtausch04()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umtausch04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0488316133"), hasWkn(null), hasTicker(null), //
                        hasName("ComStage-S&P 500 UCITS ETF Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU0496786657"), hasWkn(null), hasTicker(null), //
                        hasName("MUL-LYXOR S&P 500 UCITS ETF Inhaber-Anteile Dist USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-09-04T00:00"), hasShares(14.0369), //
                                        hasSource("Umtausch04.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2020-09-10T00:00"), hasShares(154.018), //
                                        hasSource("Umtausch04.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testFreierErhalt01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
                        hasIsin("LU0140355917"), hasWkn(null), hasTicker(null), //
                        hasName("Allianz PIMCO Euro Bd Tot.Ret. Inhaber-Anteile A (EUR) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2011-12-02T00:00"), hasShares(28.00), //
                                        hasSource("FreierErhalt01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
        assertThat(results, hasItem(deposit(hasDate("2017-04-04"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-05-03"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-01"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-02"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-26"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-06-26"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisungseingang SEPA"))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

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
        assertThat(results, hasItem(fee( //
                        hasDate("2015-04-07"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Portogebühren 03/15"), //
                        hasAmount("EUR", 0.62), hasGrossValue("EUR", 0.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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
        assertThat(results, hasItem(deposit(hasDate("2010-10-31"), hasAmount("EUR", 37.66), //
                        hasSource("Kontoauszug03.txt"), hasNote("Saldenübernahme"))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2022-03-24"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Erst. BGH-Urteil Sonstige 2. Quartal 2021"), //
                        hasAmount("EUR", 42.42), hasGrossValue("EUR", 42.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2022-03-24"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Erst. BGH-Urteil Sonstige 3. Quartal 2021"), //
                        hasAmount("EUR", 11.11), hasGrossValue("EUR", 11.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-01-03"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-01-17"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-01-19"), hasAmount("EUR", 2700.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-02-03"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-02-16"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-02-21"), hasAmount("EUR", 190.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-02-28"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungausgang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-03-16"), hasAmount("EUR", 750.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-03-21"), hasAmount("EUR", 1500.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisungausgang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2022-03-24"), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Erst. BGH-Urteil Sonstige 2. Quartal 2021"), //
                        hasAmount("EUR", 0.04), hasGrossValue("EUR", 0.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2022-03-24"), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Erst. BGH-Urteil Sonstige 3. Quartal 2021"), //
                        hasAmount("EUR", 0.12), hasGrossValue("EUR", 0.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2022-03-24"), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Erst. BGH-Urteil Sonstige 4. Quartal 2021"), //
                        hasAmount("EUR", 0.04), hasGrossValue("EUR", 0.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-10-04"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-10-18"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-10-18"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungausgang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-10-25"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungausgang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-11-03"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-11-16"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-12-03"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-12-15"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungausgang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-12-16"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisungseingang SEPA"))));
    }

    @Test
    public void testKontoauszug07()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2017-07-28"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-07-28"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-08-15"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-09-12"), hasAmount("EUR", 250.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-09-26"), hasAmount("EUR", 150.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-09-28"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-09-28"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisungseingang SEPA"))));
    }

    @Test
    public void testKontoauszug08()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

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

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-01"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-17"), hasAmount("EUR", 600.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-03-01"), hasAmount("EUR", 1100.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-03-01"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-03-25"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-03-31"), hasAmount("EUR", 150.00), //
                        hasSource("Kontoauszug08.txt"), hasNote("Überweisungseingang SEPA"))));
    }

    @Test
    public void testKontoauszug09()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-02-03"), hasAmount("EUR", 350.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-03-02"), hasAmount("EUR", 350.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-03-23"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug09.txt"), hasNote("Überweisungseingang SEPA"))));
    }

    @Test
    public void testKontoauszug10()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-01-02"), hasAmount("EUR", 66.67), //
                        hasSource("Kontoauszug10.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-02-01"), hasAmount("EUR", 66.67), //
                        hasSource("Kontoauszug10.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-03-01"), hasAmount("EUR", 66.67), //
                        hasSource("Kontoauszug10.txt"), hasNote("Überweisungseingang SEPA"))));
    }

    @Test
    public void testKontoauszug11()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-01-25"), hasAmount("EUR", 2926.19), //
                        hasSource("Kontoauszug11.txt"), hasNote("Überweisungausgang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2022-03-24"), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote("Erst. BGH-Urteil Sonstige 3. Quartal 2021"), //
                        hasAmount("EUR", 0.67), hasGrossValue("EUR", 0.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2022-03-31"), //
                        hasSource("Kontoauszug11.txt"), //
                        hasNote("Überziehungszinsen"), //
                        hasAmount("EUR", 0.77), hasGrossValue("EUR", 0.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug12()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug12.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2018-01-05"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-02-05"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-03-05"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-03-05"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug12.txt"), hasNote("Überweisungseingang SEPA"))));
    }

    @Test
    public void testKontoauszug13()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug13.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2024-04-30"), hasAmount("EUR", 30000.00), //
                        hasSource("Kontoauszug13.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-30"), hasAmount("EUR", 30000.00), //
                        hasSource("Kontoauszug13.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-05-02"), hasAmount("EUR", 7.50), //
                        hasSource("Kontoauszug13.txt"), hasNote("Geb. Back Office extern"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-22"), hasAmount("EUR", 60000.00), //
                        hasSource("Kontoauszug13.txt"), hasNote("Übertrag Referenzkonto"))));
    }

    @Test
    public void testKontoauszug14()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-07-06"), hasAmount("EUR", 1.29), //
                        hasSource("Kontoauszug14.txt"), hasNote("Geb. Back Office extern"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-08-23"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug14.txt"), hasNote("Überweisungseingang SEPA"))));
    }

    @Test
    public void testKontoauszug15()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug15.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2016-07-15"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug15.txt"), hasNote("Überweisungausgang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-07-19"), hasAmount("EUR", 5.00), //
                        hasSource("Kontoauszug15.txt"), hasNote("Überweisungseingang SEPA"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2016-08-31"), hasAmount("EUR", 0.03), //
                        hasSource("Kontoauszug15.txt"), hasNote("Überziehungszinsen"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2016-09-30"), hasAmount("EUR", 0.03), //
                        hasSource("Kontoauszug15.txt"), hasNote("Überziehungszinsen"))));
    }

    @Test
    public void testKontoauszug16()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2023-08-23"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug16.txt"), hasNote("Überweisungseingang SEPA"))));

        assertThat(results, hasItem(fee(hasDate("2023-07-06"), hasAmount("EUR", 1.29), //
                        hasSource("Kontoauszug16.txt"), hasNote("Geb. Back Office extern"))));
    }

    @Test
    public void testKontoauszug17()
    {
        var extractor = new OnvistaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        assertThat(results, hasItem(fee(hasDate("2018-04-04"), hasAmount("EUR", 0.70), //
                        hasSource("Kontoauszug17.txt"), hasNote("Portogebühren 03/18"))));

        assertThat(results, hasItem(fee(hasDate("2018-05-03"), hasAmount("EUR", 0.70), //
                        hasSource("Kontoauszug17.txt"), hasNote("Portogebühren 04/18"))));

        assertThat(results, hasItem(feeRefund(hasDate("2018-05-11"), hasAmount("EUR", 0.70), //
                        hasSource("Kontoauszug17.txt"), hasNote("Storno: Portogebühren 04/18"))));
    }
}
