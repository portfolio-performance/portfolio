package name.abuchen.portfolio.datatransfer.pdf.dab;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
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
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.DABPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class DABPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("LU0360863863"), hasWkn(null), hasTicker(null), //
                        hasName("ARERO - Der Weltfonds Inhaber-Anteile o.N."), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-01-06T00:00"), hasShares(0.91920), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Abrechnungs-Nr. 9090909090"), //
                        hasAmount("EUR", 150.00), hasGrossValue("EUR", 150.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("LU0274208692"), hasWkn(null), hasTicker(null), //
                        hasName("db x-tr.MSCI World Index ETF Inhaber-Anteile 1C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-05-04T09:15"), hasShares(1.42270), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Abrechnungs-Nr. 81119025"), //
                        hasAmount("EUR", 60.00), hasGrossValue("EUR", 55.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.95))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("LU0635178014"), hasWkn(null), hasTicker(null), //
                        hasName("ComSta.-MSCI Em.Mkts.TRN U.ETF Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-04T09:15"), hasShares(10.9468), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Abrechnungs-Nr. hintereinander)"), //
                        hasAmount("EUR", 325.00), hasGrossValue("EUR", 325.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("US8270481091"), hasWkn(null), hasTicker(null), //
                        hasName("Silgan Holdings Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-07-29T16:30"), hasShares(100.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4798.86), hasGrossValue("EUR", 4786.89), //
                        hasForexGrossValue("USD", 4786.89 * 1.100297), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 11.97))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInEUR()
    {
        var security = new Security("Silgan Holdings Inc. Registered Shares DL -,01", "EUR");
        security.setIsin("US8270481091");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

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

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-07-29T16:30"), hasShares(100.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4798.86), hasGrossValue("EUR", 4786.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 11.97))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("IE00B3F81R35"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-Core EO Corp.Bd U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-03-01T13:31"), hasShares(0.0499), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Abrechnungs-Nr. 26880356"), //
                        hasAmount("EUR", 6.46), hasGrossValue("EUR", 6.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("DE0005810055"), hasWkn(null), hasTicker(null), //
                        hasName("Deutsche Börse AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-18T14:15"), hasShares(10.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Abrechnungs-Nr. 987654321"), //
                        hasAmount("EUR", 1381.58), hasGrossValue("EUR", 1381.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.58))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("LU0635178014"), hasWkn(null), hasTicker(null), //
                        hasName("ComSta.-MSCI Em.Mkts.TRN U.ETF Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-23T12:13"), hasShares(125.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Abrechnungs-Nr. 93514089"), //
                        hasAmount("CHF", 6123.98), hasGrossValue("CHF", 6123.98), //
                        hasForexGrossValue("EUR", 6123.98 / 1.08389), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf07WithSecurityInEUR()
    {
        var security = new Security("ComSta.-MSCI Em.Mkts.TRN U.ETF Inhaber-Anteile I o.N.", "EUR");
        security.setIsin("LU0635178014");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-23T12:13"), hasShares(125.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Abrechnungs-Nr. 93514089"), //
                        hasAmount("CHF", 6123.98), hasGrossValue("CHF", 6123.98), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("US72352L1061"), hasWkn(null), hasTicker(null), //
                        hasName("Pinterest Inc. Registered Shares DL-,00001"), //
                        hasCurrencyCode("USD"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-05T16:52"), hasShares(20.00), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Abrechnungs-Nr. 1234567"), //
                        hasAmount("EUR", 1146.44), hasGrossValue("EUR", 1117.03), //
                        hasForexGrossValue("USD", 1117.03 * 1.22468), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 29.41))));
    }

    @Test
    public void testWertpapierKauf08WithSecurityInEUR()
    {
        var security = new Security("Pinterest Inc. Registered Shares DL-,00001", "EUR");
        security.setIsin("US72352L1061");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-05T16:52"), hasShares(20.00), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Abrechnungs-Nr. 1234567"), //
                        hasAmount("EUR", 1146.44), hasGrossValue("EUR", 1117.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 29.41))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();
        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

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
                        hasIsin("DE000A2LQLH9"), hasWkn(null), hasTicker(null), //
                        hasName("4,75% Ranft Invest GmbH Inh.-Schv. v.2018(2030)"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-18T08:56"), hasShares(10.00), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Abrechnungs-Nr. 123456"), //
                        hasAmount("EUR", 1029.55), hasGrossValue("EUR", 1022.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.00 + 2.24 + 0.75))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-06-18T00:00"), hasShares(10.00), //
                        hasSource("Kauf09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.95), hasGrossValue("EUR", 5.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("DE000TT649A1"), hasWkn(null), hasTicker(null), //
                        hasName("HSBC Trinkaus & Burkhardt AG DIZ Siemens 140"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-23T12:58"), hasShares(15.00), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Abrechnungs-Nr. 99999999"), //
                        hasAmount("EUR", 2003.55), hasGrossValue("EUR", 2003.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("IE00BLRPRL42"), hasWkn(null), hasTicker(null), //
                        hasName("WisdomTree Multi Ass.Iss.PLC Gas 3x Sh. ETP Secs 12(12/62)"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-11-02T09:26"), hasShares(1.00), //
                        hasSource("Kauf11.txt"), //
                        hasNote("Abrechnungs-Nr. xxxxxxxx"), //
                        hasAmount("EUR", 111.11), hasGrossValue("EUR", 110.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("DE000A28M8D0"), hasWkn(null), hasTicker(null), //
                        hasName("VanEck ETP AG ETN 31.12.29 MVCBIC"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-26T12:59"), hasShares(38.00), //
                        hasSource("Kauf12.txt"), //
                        hasNote("Abrechnungs-Nr. 123456789"), //
                        hasAmount("EUR", 693.70), hasGrossValue("EUR", 689.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.00))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("DE0008032004"), hasWkn(null), hasTicker(null), //
                        hasName("Commerzbank AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-04-07T12:34"), hasShares(449.749), //
                        hasSource("Kauf13.txt"), //
                        hasNote("Abrechnungs-Nr. [Belegnummer]"), //
                        hasAmount("EUR", 2479.12), hasGrossValue("EUR", 2469.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00))));
    }

    @Test
    public void testWertpapierKauf14()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("IE00BCBJG560"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR MSCI Wrld Small Cap U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-08-29T12:59:00"), hasShares(0.0425), //
                        hasSource("Kauf14.txt"), //
                        hasNote("Abrechnungs-Nr. 92328727"), //
                        hasAmount("EUR", 4.13), hasGrossValue("EUR", 4.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("LU0392495700"), hasWkn(null), hasTicker(null), //
                        hasName("ComStage-MSCI USA TRN UCIT.ETF Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-12-23T10:25"), hasShares(43.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Abrechnungs-Nr. 12345678"), //
                        hasAmount("EUR", 1994.12), hasGrossValue("EUR", 2056.73), //
                        hasTaxes("EUR", 45.88 + 2.52 + 4.12), hasFees("EUR", 10.09))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("US8270481091"), hasWkn(null), hasTicker(null), //
                        hasName("Silgan Holdings Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-08-24T16:38"), hasShares(100.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Abrechnungs-Nr. 00000000"), //
                        hasAmount("EUR", 4465.12), hasGrossValue("EUR", 4476.40), //
                        hasForexGrossValue("USD", 4476.40 * 1.162765), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 11.28))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2015-08-24T00:00"), hasShares(100.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 90.09), hasGrossValue("EUR", 90.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02WithSecurityInEUR()
    {
        var security = new Security("Silgan Holdings Inc. Registered Shares DL -,01", "EUR");
        security.setIsin("US8270481091");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-08-24T16:38"), hasShares(100.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Abrechnungs-Nr. 00000000"), //
                        hasAmount("EUR", 4465.12), hasGrossValue("EUR", 4476.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 11.28))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2015-08-24T00:00"), hasShares(100.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 90.09), hasGrossValue("EUR", 90.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("US8270481091"), hasWkn(null), hasTicker(null), //
                        hasName("Silgan Holdings Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-08-24T16:38"), hasShares(100.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4447.21), hasGrossValue("EUR", 4478.45), //
                        hasForexGrossValue("USD", (4447.21 + 31.24) * 1.162905 - 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 31.24))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2015-08-24T00:00"), hasShares(100.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 98.08), hasGrossValue("EUR", 98.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03WithSecurityInEUR()
    {
        var security = new Security("Silgan Holdings Inc. Registered Shares DL -,01", "EUR");
        security.setIsin("US8270481091");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-08-24T16:38"), hasShares(100.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4447.21), hasGrossValue("EUR", 4478.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 31.24))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2015-08-24T00:00"), hasShares(100.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 98.08), hasGrossValue("EUR", 98.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

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
                        hasIsin("DE000A0B65S3"), hasWkn(null), hasTicker(null), //
                        hasName("PAION AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-07-07T10:36"), hasShares(1900.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Abrechnungs-Nr. 4527275"), //
                        hasAmount("EUR", 5414.00), hasGrossValue("EUR", 5415.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2020-07-07T00:00"), hasShares(1900.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.46), hasGrossValue("EUR", 16.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

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
                        hasIsin("SE0014855029"), hasWkn(null), hasTicker(null), //
                        hasName("Implantica AG Reg.Sw.Dep.Rcpts (SDRs)/1 o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-09T13:04"), hasShares(220.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Abrechnungs-Nr. 123456"), //
                        hasAmount("EUR", 2577.52), hasGrossValue("EUR", 2577.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("US09075V1026"), hasWkn(null), hasTicker(null), //
                        hasName("BioNTech SE Nam.-Akt.(sp.ADRs)1/o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-06-16T10:12"), hasShares(100.00), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("Abrechnungs-Nr. 98765432"), //
                        hasAmount("EUR", 17509.00), hasGrossValue("EUR", 17510.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-06-16T00:00"), hasShares(100.00), //
                        hasSource("Verkauf06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 178.59), hasGrossValue("EUR", 178.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf07()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

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
                        hasIsin("NL0015436031"), hasWkn(null), hasTicker(null), //
                        hasName("CureVac N.V. Namensaktien   o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-05-31T08:50"), hasShares(300.00), //
                        hasSource("Verkauf07.txt"), //
                        hasNote("Abrechnungs-Nr. 12106108"), //
                        hasAmount("EUR", 26252.00), hasGrossValue("EUR", 26253.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-05-31T00:00"), hasShares(300.00), //
                        hasSource("Verkauf07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 551.08), hasGrossValue("EUR", 551.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf08()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf08.txt"), errors);

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
                        hasIsin("NL0015436031"), hasWkn(null), hasTicker(null), //
                        hasName("CureVac N.V. Namensaktien   o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-05-26T09:01"), hasShares(300.00), //
                        hasSource("Verkauf08.txt"), //
                        hasNote("Abrechnungs-Nr. 43214321"), //
                        hasAmount("EUR", 27911.45), hasGrossValue("EUR", 28293.00), //
                        hasTaxes("EUR", 335.29 + 18.44 + 26.82), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierVerkauf09()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0015436031"), hasWkn(null), hasTicker(null), //
                        hasName("CureVac N.V. Namensaktien   o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-05-31T08:50"), hasShares(300.00), //
                        hasSource("Verkauf09.txt"), //
                        hasNote("Abrechnungs-Nr. 12106108"), //
                        hasAmount("EUR", 26252.00), hasGrossValue("EUR", 26253.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));

        // check tax refund for 1st transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-05-31T00:00"), hasShares(300.00), //
                        hasSource("Verkauf09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 551.08), hasGrossValue("EUR", 551.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-05-26T09:01"), hasShares(300.00), //
                        hasSource("Verkauf09.txt"), //
                        hasNote("Abrechnungs-Nr. 43214321"), //
                        hasAmount("EUR", 27911.45), hasGrossValue("EUR", 28293.00), //
                        hasTaxes("EUR", 335.29 + 18.44 + 26.82), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierVerkauf10()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("DE000HB0NKY1"), hasWkn(null), hasTicker(null), //
                        hasName("UniCredit Bank AG HVB TuBull O.EndDJIA34745,0898"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-12-07T00:00"), hasShares(54), //
                        hasSource("Verkauf10.txt"), //
                        hasNote("Abrechnungs-Nr. 00000000"), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf11()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("DE000GX3ZTT5"), hasWkn(null), hasTicker(null), //
                        hasName("Goldman Sachs Bank Europe SE TuBull O.End Nasd100 16416,89"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-12-03T00:00"), hasShares(783), //
                        hasSource("Verkauf11.txt"), //
                        hasNote("Abrechnungs-Nr. 00000000"), //
                        hasAmount("EUR", 0.78), hasGrossValue("EUR", 0.78), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-12-03T00:00"), hasShares(783), //
                        hasSource("Verkauf11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 142.93), hasGrossValue("EUR", 142.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf12()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("CA32076V1031"), hasWkn(null), hasTicker(null), //
                        hasName("First Majestic Silver Corp. Registered Shares o.N."), //
                        hasCurrencyCode("CAD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-02-18T16:56"), hasShares(100), //
                        hasSource("Verkauf12.txt"), //
                        hasNote("Abrechnungs-Nr. 99887755"), //
                        hasAmount("EUR", 1013.54), hasGrossValue("EUR", 1032.88), //
                        hasForexGrossValue("CAD", 1499.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 19.34))));
    }

    @Test
    public void testWertpapierVerkauf12WithSecurityInEUR()
    {
        var security = new Security("First Majestic Silver Corp. Registered Shares o.N.", "EUR");
        security.setIsin("CA32076V1031");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf12.txt"), errors);

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
                        hasDate("2022-02-18T16:56"), hasShares(100), //
                        hasSource("Verkauf12.txt"), //
                        hasNote("Abrechnungs-Nr. 99887755"), //
                        hasAmount("EUR", 1013.54), hasGrossValue("EUR", 1032.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 19.34))));
    }

    @Test
    public void testWertpapierVerkauf13()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("DE0001104875"), hasWkn(null), hasTicker(null), //
                        hasName("Bundesrep.Deutschland Bundesschatzanw. v.22(24)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-03-15T00:00"), hasShares(100), //
                        hasSource("Verkauf13.txt"), //
                        hasNote("Abrechnungs-Nr. 12345678"), //
                        hasAmount("EUR", 9880.00), hasGrossValue("EUR", 10000.00), //
                        hasTaxes("EUR", 100.00 + 10.00 + 10.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf14()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf14.txt"), errors);

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
                        hasIsin("IE00BL25JM42"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE) - MSCI World Value Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-11-07T16:00"), hasShares(0.2741), //
                        hasSource("Verkauf14.txt"), //
                        hasNote("Abrechnungs-Nr. 64518224"), //
                        hasAmount("EUR", 11.60), hasGrossValue("EUR", 11.67), //
                        hasTaxes("EUR", 0.07), hasFees("EUR", 0.00))));
    }

    @Test
    public void testMultipleWertpapierKaufVerkauf01()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();
        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "MultipleKaufVerkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(6L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, "EUR");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin("US01609W1027"), hasWkn(null), hasTicker(null), //
                        hasName("Alibaba Group Holding Ltd. Reg.Shs (sp.ADRs)/8 DL-,000025"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US34959E1091"), hasWkn(null), hasTicker(null), //
                        hasName("Fortinet Inc. Registered Shares DL -,001"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US09247X1019"), hasWkn(null), hasTicker(null), //
                        hasName("Blackrock Inc. Reg. Shares Class A DL -,01"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US8447411088"), hasWkn(null), hasTicker(null), //
                        hasName("Southwest Airlines Co. Registered Shares DL 1"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US8036071004"), hasWkn(null), hasTicker(null), //
                        hasName("Sarepta Therapeutics Inc. Registered Shares DL -,0001"), //
                        hasCurrencyCode("EUR"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-30T09:59"), hasShares(3.00), //
                        hasSource("MultipleKaufVerkauf01.txt"), //
                        hasNote("Abrechnungs-Nr. "), //
                        hasAmount("EUR", 679.50), hasGrossValue("EUR", 679.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transactions
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-27T16:08"), hasShares(6.00), //
                        hasSource("MultipleKaufVerkauf01.txt"), //
                        hasNote("Abrechnungs-Nr. 15242954"), //
                        hasAmount("EUR", 619.92), hasGrossValue("EUR", 619.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-24T19:25"), hasShares(1.00), //
                        hasSource("MultipleKaufVerkauf01.txt"), //
                        hasNote("Abrechnungs-Nr. 13143056"), //
                        hasAmount("EUR", 585.70), hasGrossValue("EUR", 585.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-24T14:21"), hasShares(14.00), //
                        hasSource("MultipleKaufVerkauf01.txt"), //
                        hasNote("Abrechnungs-Nr. 95588332"), //
                        hasAmount("EUR", 573.02), hasGrossValue("EUR", 573.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-20T20:41"), hasShares(3.00), //
                        hasSource("MultipleKaufVerkauf01.txt"), //
                        hasNote("Abrechnungs-Nr. 74216527"), //
                        hasAmount("EUR", 685.50), hasGrossValue("EUR", 685.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-20T15:32"), hasShares(5.00), //
                        hasSource("MultipleKaufVerkauf01.txt"), //
                        hasNote("Abrechnungs-Nr. 70044098"), //
                        hasAmount("EUR", 565.10), hasGrossValue("EUR", 564.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));
    }

    @Test
    public void testWertpapierVerkauf_Steuerkorrektur01()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf_Steuerkorrektur01.txt"), errors);

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
                        hasIsin("LU1602145119"), hasWkn(null), hasTicker(null), //
                        hasName("AIS-Am.I.Eq.Gl.M.Sm.Allo.Sc.B. Act.Nom.Uc.ETF DR EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-01-17T00:00"), hasShares(29.00), //
                        hasSource("Verkauf_Steuerkorrektur01.txt"), //
                        hasNote("Abrechnungs-Nr. 50345944 | Steuerliche Korrektur Abrechnung Nr. 55935348 vom 09.01.2025"), //
                        hasAmount("EUR", 47.56), hasGrossValue("EUR", 47.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("DE0005660104"), hasWkn(null), hasTicker(null), //
                        hasName("EUWAX AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-07-02T00:00"), hasExDate("2014-07-02T00:00"), //
                        hasShares(100), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 326.00), hasGrossValue("EUR", 326.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new DABPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7427181091"), hasWkn(null), hasTicker(null), //
                        hasName("Procter & Gamble Co., The Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-05-16T00:00"), hasExDate("2015-04-14T00:00"), //
                        hasShares(100), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungs-Nr. 989898989"), //
                        hasAmount("USD", 56.91), hasGrossValue("USD", 56.91), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        var security = new Security("Procter & Gamble Co., The Registered Shares o.N.", "EUR");
        security.setIsin("US7427181091");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

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
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-05-16T00:00"), hasExDate("2015-04-14T00:00"), //
                        hasShares(100), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungs-Nr. 989898989"), //
                        hasAmount("USD", 56.91), hasGrossValue("USD", 56.91), //
                        hasForexGrossValue("EUR", 56.91 * 1.1409), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("ZAE000042164"), hasWkn(null), hasTicker(null), //
                        hasName("MTN Group Ltd. Registered Shares RC -,0001"), //
                        hasCurrencyCode("ZAR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-03-30T00:00"), hasExDate("2015-03-23T00:00"), //
                        hasShares(1300), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 586.80), hasGrossValue("EUR", 788.18), //
                        hasForexGrossValue("ZAR", 10400.04), //
                        hasTaxes("EUR", 201.38), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("MTN Group Ltd. Registered Shares RC -,0001", "EUR");
        security.setIsin("ZAE000042164");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

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
                        hasDate("2015-03-30T00:00"), hasExDate("2015-03-23T00:00"), //
                        hasShares(1300), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 586.80), hasGrossValue("EUR", 788.18), //
                        hasTaxes("EUR", 201.38), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("DE0006483001"), hasWkn(null), hasTicker(null), //
                        hasName("Linde AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2013-05-31T00:00"), hasExDate("2013-05-30T00:00"), //
                        hasShares(100), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 198.79), hasGrossValue("EUR", 270.00), //
                        hasTaxes("EUR", 71.21), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new DABPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7043261079"), hasWkn(null), hasTicker(null), //
                        hasName("Paychex Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-27T00:00"), hasExDate("2020-07-31T00:00"), //
                        hasShares(10), //
                        hasSource("Dividende05.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 4.64), hasGrossValue("USD", 5.77), //
                        hasTaxes("USD", (0.79 + 0.52 + 0.02) / 1.1814), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende05WithSecurityinEUR()
    {
        var security = new Security("Paychex Inc. Registered Shares DL -,01", "EUR");
        security.setIsin("US7043261079");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

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
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-27T00:00"), hasExDate("2020-07-31T00:00"), //
                        hasShares(10), //
                        hasSource("Dividende05.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 4.64), hasGrossValue("USD", 5.77), //
                        hasForexGrossValue("EUR", 5.77 * 1.1814), //
                        hasTaxes("USD", (0.79 + 0.52 + 0.02) / 1.1814), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("US92826C8394"), hasWkn(null), hasTicker(null), //
                        hasName("VISA Inc. Reg. Shares Class A DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-09-01T00:00"), hasExDate("2020-08-13T00:00"), //
                        hasShares(4), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Abrechnungs-Nr. 123456789"), //
                        hasAmount("EUR", 0.76), hasGrossValue("EUR", 1.01), //
                        hasForexGrossValue("USD", 1.20), //
                        hasTaxes("EUR", 0.25), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06WithSecurityinEUR()
    {
        var security = new Security("VISA Inc. Reg. Shares Class A DL -,0001", "EUR");
        security.setIsin("US92826C8394");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

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
                        hasDate("2020-09-01T00:00"), hasExDate("2020-08-13T00:00"), //
                        hasShares(4), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Abrechnungs-Nr. 123456789"), //
                        hasAmount("EUR", 0.76), hasGrossValue("EUR", 1.01), //
                        hasTaxes("EUR", 0.25), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new DABPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4404521001"), hasWkn("850875"), hasTicker(null), //
                        hasName("HORMEL FOODS CORP. Registered Shares DL 0,01465"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-02-15T00:00"), hasExDate(null), //
                        hasShares(1500), //
                        hasSource("Dividende07.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 209.38), hasGrossValue("USD", 281.25), //
                        hasTaxes("USD", 71.87), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende07WithSecurityinEUR()
    {
        var security = new Security("HORMEL FOODS CORP. Registered Shares DL 0,01465", "EUR");
        security.setIsin("US4404521001");
        security.setWkn("850875");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();
        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-02-15T00:00"), hasExDate(null), //
                        hasShares(1500), //
                        hasSource("Dividende07.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 209.38), hasGrossValue("USD", 281.25), //
                        hasForexGrossValue("EUR", 225.02), //
                        hasTaxes("USD", 71.87), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new DABPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BJ5JP329"), hasWkn(null), hasTicker(null), //
                        hasName("iShs V-MSCI W.C.St.Sec.U.ETF Reg. Shs USD Dis. oN"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-30T00:00"), hasExDate("2021-06-17T00:00"), //
                        hasShares(315), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Abrechnungs-Nr. 81234456789"), //
                        hasAmount("USD", 20.49), hasGrossValue("USD", 25.11), //
                        hasTaxes("USD", (3.68 + 0.20) * 1.1914), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende08WithSecurityinEUR()
    {
        var security = new Security("iShs V-MSCI W.C.St.Sec.U.ETF Reg. Shs USD Dis. oN", "EUR");
        security.setIsin("IE00BJ5JP329");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

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
        new AssertImportActions().check(results, "USD");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-30T00:00"), hasExDate("2021-06-17T00:00"), //
                        hasShares(315), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Abrechnungs-Nr. 81234456789"), //
                        hasAmount("USD", 20.49), hasGrossValue("USD", 25.11), //
                        hasForexGrossValue("EUR", 25.11 / 1.1914), //
                        hasTaxes("USD", (3.68 + 0.20) * 1.1914), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("IE00B3F81R35"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-Core EO Corp.Bd U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-01-27T00:00"), hasExDate("2017-01-12T00:00"), //
                        hasShares(3.4256), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Abrechnungs-Nr. <AbrNr>"), //
                        hasAmount("EUR", 3.47), hasGrossValue("EUR", 3.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("LU0480132876"), hasWkn(null), hasTicker(null), //
                        hasName("UBS-ETF - UBS-ETF MSCI Em.Mkts Inhaber-Anteile A o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-02-07T00:00"), hasExDate("2017-02-02T00:00"), //
                        hasShares(14.3755), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Abrechnungs-Nr. <AbrNr>"), //
                        hasAmount("EUR", 11.06), hasGrossValue("EUR", 14.81), //
                        hasForexGrossValue("USD", 14.81 * 1.0797), //
                        hasTaxes("EUR", 3.75), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("JP3756600007"), hasWkn(null), hasTicker(null), //
                        hasName("Nintendo Co. Ltd. Registered Shares o.N."), //
                        hasCurrencyCode("JPY"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-12-01T00:00"), hasExDate("2021-09-29T00:00"), //
                        hasShares(1), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Abrechnungs-Nr. 93130690"), //
                        hasAmount("EUR", 3.59), hasGrossValue("EUR", 4.84), //
                        hasForexGrossValue("JPY", 619.47), //
                        hasTaxes("EUR", ((95 / 127.99) + 0.48 + 0.03)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11WithSecurityInJPY()
    {
        var security = new Security("Nintendo Co. Ltd. Registered Shares o.N.", "JPY");
        security.setIsin("JP3756600007");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

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
                        hasDate("2021-12-01T00:00"), hasExDate("2021-09-29T00:00"), //
                        hasShares(1), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Abrechnungs-Nr. 93130690"), //
                        hasAmount("EUR", 3.59), hasGrossValue("EUR", 4.84), //
                        hasTaxes("EUR", ((95 / 127.99) + 0.48 + 0.03)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("US5801351017"), hasWkn(null), hasTicker(null), //
                        hasName("McDonald's Corp. Registered Shares DL-,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2013-12-16T00:00"), hasExDate("2013-11-27T00:00"), //
                        hasShares(20), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Abrechnungs-Nr. 12345678"), //
                        hasAmount("EUR", 9.95), hasGrossValue("EUR", 9.95 + (2.430 / 1.3841)), //
                        hasForexGrossValue("USD", 16.19), //
                        hasTaxes("EUR", 2.430 / 1.3841), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12WithSecurityinEUR()
    {
        var security = new Security("McDonald's Corp. Registered Shares DL-,01", "EUR");
        security.setIsin("US5801351017");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

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
                        hasDate("2013-12-16T00:00"), hasExDate("2013-11-27T00:00"), //
                        hasShares(20), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Abrechnungs-Nr. 12345678"), //
                        hasAmount("EUR", 9.95), hasGrossValue("EUR", 9.95 + (2.430 / 1.3841)), //
                        hasTaxes("EUR", 2.430 / 1.3841), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende13()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("US74348T1025"), hasWkn("A0B746"), hasTicker(null), //
                        hasName("Prospect Capital Corp. Registered Shares DL -,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-21T00:00"), hasExDate(null), //
                        hasShares(10000), //
                        hasSource("Dividende13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 450.84), hasGrossValue("EUR", 605.57), //
                        hasForexGrossValue("USD", 600.00), //
                        hasTaxes("EUR", 90.83 + 60.57 + 3.33), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende13WithSecurityinEUR()
    {
        var security = new Security("Prospect Capital Corp. Registered Shares DL -,001", "EUR");
        security.setIsin("US74348T1025");
        security.setWkn("A0B746");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();
        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

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
                        hasDate("2022-09-21T00:00"), hasExDate(null), //
                        hasShares(10000), //
                        hasSource("Dividende13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 450.84), hasGrossValue("EUR", 605.57), //
                        hasTaxes("EUR", 90.83 + 60.57 + 3.33), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("US8288061091"), hasWkn("916647"), hasTicker(null), //
                        hasName("SIMON PROPERTY GROUP INC. Reg. Paired Shares DL-,0001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-30T00:00"), hasExDate(null), //
                        hasShares(850), //
                        hasSource("Dividende14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1119.09), hasGrossValue("EUR", 1516.31), //
                        hasForexGrossValue("USD", 1487.50), //
                        hasTaxes("EUR", 227.45 + 148.29 + 8.14 + 13.34), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14WithSecurityinEUR()
    {
        var security = new Security("SIMON PROPERTY GROUP INC. Reg. Paired Shares DL-,0001", "EUR");
        security.setIsin("US8288061091");
        security.setWkn("916647");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

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
                        hasDate("2022-09-30T00:00"), hasExDate(null), //
                        hasShares(850), //
                        hasSource("Dividende14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1119.09), hasGrossValue("EUR", 1516.31), //
                        hasTaxes("EUR", 227.45 + 148.29 + 8.14 + 13.34), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("US2538681030"), hasWkn(null), hasTicker(null), //
                        hasName("Digital Realty Trust Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-29T00:00"), hasExDate("2023-09-14T00:00"), //
                        hasShares(25), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Abrechnungs-Nr. 12345678"), //
                        hasAmount("EUR", 21.38), hasGrossValue("EUR", 28.73), //
                        hasForexGrossValue("USD", 30.50), //
                        hasTaxes("EUR", 4.31 + 2.88 + 0.16), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15WithSecurityInEUR()
    {
        var security = new Security("Digital Realty Trust Inc. Registered Shares DL -,01", "EUR");
        security.setIsin("US2538681030");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DABPDFExtractor(client);

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
                        hasDate("2023-09-29T00:00"), hasExDate("2023-09-14T00:00"), //
                        hasShares(25), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Abrechnungs-Nr. 12345678"), //
                        hasAmount("EUR", 21.38), hasGrossValue("EUR", 28.73), //
                        hasTaxes("EUR", 4.31 + 2.88 + 0.16), hasFees("EUR", 0.00))));
    }

    @Test
    public void testEinbuchung01()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Einbuchung01.txt"), errors);

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
                        hasIsin("DE0006926504"), hasWkn(null), hasTicker(null), //
                        hasName("Solutiance AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2021-03-11T00:00"), hasShares(20.00), //
                        hasSource("Einbuchung01.txt"), //
                        hasNote("Abrechnungs-Nr. 84747170"), //
                        hasAmount("EUR", 27.50), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50 + 27.00))));
    }

    @Test
    public void testSplit01()
    {
        var extractor = new DABPDFExtractor(new Client());

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
                        hasIsin("LU1291106356"), hasWkn(null), hasTicker(null), //
                        hasName("BNP P.Easy-MSCI Pac.x.Jap.x.CW Nam.-Ant.UCITS ETF CAP o.N"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        inboundDelivery( //
                                        hasDate("2018-12-04T00:00"), hasShares(1.5884), //
                                        hasSource("Split01.txt"), //
                                        hasNote("Ausführungs-Nr. 12345678"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testAusbuchung01()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Ausbuchung01.txt"), errors);

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
                        hasIsin("AU000000CFU6"), hasWkn(null), hasTicker(null), //
                        hasName("Ceramic Fuel Cells Ltd. Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2022-02-04T00:00"), hasShares(1000.00), //
                                        hasSource("Ausbuchung01.txt"), //
                                        hasNote("Ausführungs-Nr. 62772656"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testSteuerausgleich01()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerausgleich01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-05-05T00:00"), hasShares(0.00), //
                        hasSource("Steuerausgleich01.txt"), //
                        hasNote("Abrechnungs-Nr. 12345678 | Steuerausgleich 2023"), //
                        hasAmount("EUR", 143.90), hasGrossValue("EUR", 143.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

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
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-01-15T00:00"), hasShares(50.00), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote("Abrechnungs-Nr. 92956682"), //
                        hasAmount("EUR", 11.43), hasGrossValue("EUR", 11.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoumsaetze01()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-07-05"), hasAmount("EUR", 15000.00), //
                        hasSource("Kontoumsaetze01.txt"), hasNote("SEPA-Gutschrift"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-07-15"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoumsaetze01.txt"), hasNote("SEPA-Lastschrift"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-07-17"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoumsaetze01.txt"), hasNote("SEPA-Gutschrift"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-07-17"), hasAmount("EUR", 1900.00), //
                        hasSource("Kontoumsaetze01.txt"), hasNote("SEPA-Gutschrift"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-08-13"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoumsaetze01.txt"), hasNote("SEPA-Lastschrift"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2019-09-13"), hasAmount("EUR", 300.00), //
                        hasSource("Kontoumsaetze01.txt"), hasNote("SEPA-Lastschrift"))));
    }

    @Test
    public void testKontoumsaetze02()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-13"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoumsaetze02.txt"), hasNote("SEPA-Lastschrift"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-18"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoumsaetze02.txt"), hasNote("SEPA-Lastschrift"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2021-03-15"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoumsaetze02.txt"), hasNote("SEPA-Lastschrift"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-06-29"), //
                        hasSource("Kontoumsaetze02.txt"), //
                        hasNote("Managementgebühr"), //
                        hasAmount("EUR", 53.02), hasGrossValue("EUR", 53.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-06-29"), //
                        hasSource("Kontoumsaetze02.txt"), //
                        hasNote("Managementgebühr"), //
                        hasAmount("EUR", 23.05), hasGrossValue("EUR", 23.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoumsaetze03()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-09-23"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoumsaetze03.txt"), hasNote("SEPA-Überweisung"))));
    }

    @Test
    public void testKontoumsaetze04()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2022-11-02"), hasAmount("EUR", 44.00), //
                        hasSource("Kontoumsaetze04.txt"), hasNote("SEPA-Überweisung"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2022-11-15"), hasAmount("EUR", 7.99), //
                        hasSource("Kontoumsaetze04.txt"), hasNote("SEPA-Überweisung"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2022-11-18"), hasAmount("EUR", 212.00), //
                        hasSource("Kontoumsaetze04.txt"), hasNote("SEPA-Überweisung"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2022-11-22"), hasAmount("EUR", 36.00), //
                        hasSource("Kontoumsaetze04.txt"), hasNote("SEPA-Überweisung"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2022-11-25"), hasAmount("EUR", 166.00), //
                        hasSource("Kontoumsaetze04.txt"), hasNote("SEPA-Überweisung"))));
    }

    @Test
    public void testKontoumsaetze05()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-09-27"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoumsaetze05.txt"), hasNote("SEPA-Überweisung"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-09-28"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoumsaetze05.txt"), hasNote("SEPA-Überweisung"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2022-09-22"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoumsaetze05.txt"), hasNote("SEPA-Überweisung"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2022-09-27"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoumsaetze05.txt"), hasNote("SEPA-Überweisung"))));

        // check transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2022-09-30"), //
                        hasSource("Kontoumsaetze05.txt"), //
                        hasNote("Sollzinsen"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoumsaetze06()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2015-04-02"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoumsaetze06.txt"), hasNote("SEPA-Gutschrift"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2015-05-05"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoumsaetze06.txt"), hasNote("SEPA-Gutschrift"))));

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2015-06-02"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoumsaetze06.txt"), hasNote("SEPA-Gutschrift"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-07"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoumsaetze06.txt"), hasNote("SEPA-Überweisung"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2015-04-10"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoumsaetze06.txt"), hasNote("SEPA-Dauerauftrag"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2015-05-11"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoumsaetze06.txt"), hasNote("SEPA-Dauerauftrag"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2015-06-10"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoumsaetze06.txt"), hasNote("SEPA-Dauerauftrag"))));
    }

    @Test
    public void testKontoumsaetze07()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze07.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2016-04-04"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoumsaetze07.txt"), hasNote("SEPA-Gutschrift"))));

        // check transaction
        assertThat(results, hasItem(removal(hasDate("2016-04-11"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoumsaetze07.txt"), hasNote("SEPA-Dauerauftrag"))));

        // check transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2016-04-05"), //
                        hasSource("Kontoumsaetze07.txt"), //
                        hasNote("Porto"), //
                        hasAmount("EUR", 0.70), hasGrossValue("EUR", 0.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoumsaetze08()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze08.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2024-08-28"), hasAmount("EUR", 26.59), //
                        hasSource("Kontoumsaetze08.txt"), hasNote("vermögenswirksame Leistung"))));
    }

    @Test
    public void testKontoumsaetze09()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        fee( //
                                        hasDate("2024-10-09"), //
                                        hasSource("Kontoumsaetze09.txt"), //
                                        hasNote("Ginmon Gebuehrenrechnung September 2024"), //
                                        hasAmount("EUR", 0.02)))));
    }

    @Test
    public void testKontoumsaetze10()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze10.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2024-11-11"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoumsaetze10.txt"), hasNote("SEPA-Überweisung"))));
    }

    @Test
    public void testKontoumsaetze11()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B52MJY50"), hasWkn(null), hasTicker(null), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        sale( //
                                        hasDate("2024-11-11"), hasShares(0.003), //
                                        hasSource("Kontoumsaetze11.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.49), hasGrossValue("EUR", 0.49), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKontoumsaetze12()
    {
        var extractor = new DABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoumsaetze12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JM42"), hasWkn(null), hasTicker(null), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        purchase(//
                                        hasDate("2024-10-01"), hasShares(0.15), //
                                        hasSource("Kontoumsaetze12.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 6.26), hasGrossValue("EUR", 6.26), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }
}
