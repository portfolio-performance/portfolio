package name.abuchen.portfolio.datatransfer.pdf.findependentag;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.feeRefund;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
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

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.FindependentAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class FindependentAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE All World ETF"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-01T00:00"), hasShares(2), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 200.43), hasGrossValue("CHF", 200.08), //
                        hasTaxes("CHF", 0.30), hasFees("CHF", 0.05))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B53SZB19"), hasWkn(null), hasTicker(null), //
                        hasName("iShares NASDAQ 100 ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-12T00:00"), hasShares(1), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 800.19), hasGrossValue("CHF", 798.94), //
                        hasForexGrossValue("USD", 912.50), //
                        hasTaxes("CHF", 1.20), hasFees("CHF", 0.05))));
    }

    @Test
    public void testWertpapierKauf02WithSecurityInCHF()
    {
        Security security = new Security("iShares NASDAQ 100 ETF", "CHF");
        security.setIsin("IE00B53SZB19");

        Client client = new Client();
        client.addSecurity(security);

        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-12T00:00"), hasShares(1), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 800.19), hasGrossValue("CHF", 798.94), //
                        hasTaxes("CHF", 1.20), hasFees("CHF", 0.05), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFNM3G45"), hasWkn(null), hasTicker(null), //
                        hasName("iShares MSCI USA ESG Screened ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(117), //
                        hasSource("Kauf03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 912.74), hasGrossValue("CHF", 911.32), //
                        hasForexGrossValue("USD", 1039.19), //
                        hasTaxes("CHF", 1.37), hasFees("CHF", 0.05))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInCHF()
    {
        Security security = new Security("iShares MSCI USA ESG Screened ETF", "CHF");
        security.setIsin("IE00BFNM3G45");

        Client client = new Client();
        client.addSecurity(security);

        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(117), //
                        hasSource("Kauf03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 912.74), hasGrossValue("CHF", 911.32), //
                        hasTaxes("CHF", 1.37), hasFees("CHF", 0.05), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0105994401"), hasWkn(null), hasTicker(null), //
                        hasName("UBS SXI Real Estate Funds ETF"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(58), //
                        hasSource("Kauf04.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 491.16), hasGrossValue("CHF", 490.74), //
                        hasTaxes("CHF", 0.37), hasFees("CHF", 0.05))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFNM3D14"), hasWkn(null), hasTicker(null), //
                        hasName("iShares MSCI Europe ESG Screened ETF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(54), //
                        hasSource("Kauf05.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 378.55), hasGrossValue("CHF", 377.93), //
                        hasForexGrossValue("EUR", 392.69), //
                        hasTaxes("CHF", 0.57), hasFees("CHF", 0.05))));
    }

    @Test
    public void testWertpapierKauf05WithSecurityInCHF()
    {
        Security security = new Security("iShares MSCI Europe ESG Screened ETF", "CHF");
        security.setIsin("IE00BFNM3D14");

        Client client = new Client();
        client.addSecurity(security);

        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(54), //
                        hasSource("Kauf05.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 378.55), hasGrossValue("CHF", 377.93), //
                        hasTaxes("CHF", 0.57), hasFees("CHF", 0.05), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFNM3L97"), hasWkn(null), hasTicker(null), //
                        hasName("iShares MSCI Japan ESG Screened ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(26), //
                        hasSource("Kauf06.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 134.00), hasGrossValue("CHF", 133.75), //
                        hasForexGrossValue("USD", 152.52), //
                        hasTaxes("CHF", 0.20), hasFees("CHF", 0.05))));
    }

    @Test
    public void testWertpapierKauf06WithSecurityInCHF()
    {
        Security security = new Security("iShares MSCI Japan ESG Screened ETF", "CHF");
        security.setIsin("IE00BFNM3L97");

        Client client = new Client();
        client.addSecurity(security);

        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(26), //
                        hasSource("Kauf06.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 134.00), hasGrossValue("CHF", 133.75), //
                        hasTaxes("CHF", 0.20), hasFees("CHF", 0.05), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFNM3P36"), hasWkn(null), hasTicker(null), //
                        hasName("iShares MSCI Emerging Markets ESG Screened ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(61), //
                        hasSource("Kauf07.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 317.42), hasGrossValue("CHF", 316.89), //
                        hasForexGrossValue("USD", 361.36), //
                        hasTaxes("CHF", 0.48), hasFees("CHF", 0.05))));
    }

    @Test
    public void testWertpapierKauf07WithSecurityInCHF()
    {
        Security security = new Security("iShares MSCI Emerging Markets ESG Screened ETF", "CHF");
        security.setIsin("IE00BFNM3P36");

        Client client = new Client();
        client.addSecurity(security);

        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(61), //
                        hasSource("Kauf07.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 317.42), hasGrossValue("CHF", 316.89), //
                        hasTaxes("CHF", 0.48), hasFees("CHF", 0.05), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0237935652"), hasWkn(null), hasTicker(null), //
                        hasName("iShares Core SPI ETF"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(7), //
                        hasSource("Kauf08.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 953.04), hasGrossValue("CHF", 952.28), //
                        hasTaxes("CHF", 0.71), hasFees("CHF", 0.05))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0226976816"), hasWkn(null), hasTicker(null), //
                        hasName("iShares Core CHF Corp Bond ETF"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(12), //
                        hasSource("Kauf09.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1105.71), hasGrossValue("CHF", 1104.58), //
                        hasTaxes("CHF", 0.83), hasFees("CHF", 0.30))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF553838"), hasWkn(null), hasTicker(null), //
                        hasName("iShares J.P. Morgan ESG USD EM Bond ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(60), //
                        hasSource("Kauf10.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 265.27), hasGrossValue("CHF", 264.82), //
                        hasForexGrossValue("USD", 301.98), //
                        hasTaxes("CHF", 0.40), hasFees("CHF", 0.05))));
    }

    @Test
    public void testWertpapierKauf10WithSecurityInCHF()
    {
        Security security = new Security("iShares J.P. Morgan ESG USD EM Bond ETF", "CHF");
        security.setIsin("IE00BF553838");

        Client client = new Client();
        client.addSecurity(security);

        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(60), //
                        hasSource("Kauf10.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 265.27), hasGrossValue("CHF", 264.82), //
                        hasTaxes("CHF", 0.40), hasFees("CHF", 0.05), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0130595124"), hasWkn(null), hasTicker(null), //
                        hasName("UBS SPI Mid ETF"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-11T00:00"), hasShares(3), //
                        hasSource("Kauf11.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 344.95), hasGrossValue("CHF", 344.64), //
                        hasTaxes("CHF", 0.26), hasFees("CHF", 0.05))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE All World ETF"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-12-12T00:00"), hasShares(7), //
                        hasSource("Verkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 700.02), hasGrossValue("CHF", 701.12), //
                        hasTaxes("CHF", 1.05), hasFees("CHF", 0.05))));
    }

    @Test
    public void testDividende01()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0105994401"), hasWkn(null), hasTicker(null), //
                        hasName("UBS SXI Real Estate Funds ETF"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-13T00:00"), hasShares(58), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 4.52), hasGrossValue("CHF", 6.96), //
                        hasTaxes("CHF", 2.44), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0105994401"), hasWkn(null), hasTicker(null), //
                        hasName("UBS SXI Real Estate Funds ETF"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-13T00:00"), hasShares(58), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 5.22), hasGrossValue("CHF", 5.22), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testEinzahlung01()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Einzahlung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-11-06"), hasAmount("CHF", 5100.00), //
                        hasSource("Einzahlung01.txt"), hasNote("SE-2"))));
    }

    @Test
    public void testEinzahlung02()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Einzahlung02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-08-08"), hasAmount("CHF", 5000.00), //
                        hasSource("Einzahlung02.txt"), hasNote("Überweisung auf findependent Konto"))));
    }

    @Test
    public void testEinzahlung03()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Einzahlung03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-08-10"), hasAmount("CHF", 40.00), //
                        hasSource("Einzahlung03.txt"), hasNote("Willkommensbonus"))));
    }

    @Test
    public void testDepotgebuehren01()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotgebuehren01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-09-29"), hasAmount("CHF", 1.95), //
                        hasSource("Depotgebuehren01.txt"), hasNote("Depotgebühren 01.07.2023 - 30.09.2023"))));
    }

    @Test
    public void testVerwaltungsgebuehren01()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verwaltungsgebuehren01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-10-10"), hasAmount("CHF", 2.45), //
                        hasSource("Verwaltungsgebuehren01.txt"), hasNote("Verwaltungsgebühren 01.10.2023 - 31.12.2023"))));
    }

    @Test
    public void testGebuehrenerstattung01()
    {
        FindependentAGPDFExtractor extractor = new FindependentAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Gebuehrenerstattung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2023-10-10"), hasAmount("CHF", 2.20), //
                        hasSource("Gebuehrenerstattung01.txt"), hasNote("findependent Gutschrift Q3"))));
    }
}
