package name.abuchen.portfolio.datatransfer.pdf.hypothekarbanklenzburgag;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
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
import name.abuchen.portfolio.datatransfer.pdf.HypothekarbankLenzburgAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class HypothekarbankLenzburgAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
                        hasIsin("IE000716YHJ7"), hasWkn("125615212"), hasTicker(null), //
                        hasName("Inve FTSE All"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-14T00:00"), hasShares(720.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Transaktion 61327806-0002"), //
                        hasAmount("CHF", 3974.14), hasGrossValue("CHF", 3948.48), //
                        hasTaxes("CHF", 5.92), hasFees("CHF", 19.74))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
                        hasIsin("IE000716YHJ7"), hasWkn("125615212"), hasTicker(null), //
                        hasName("Inve FTSE All"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-04T00:00"), hasShares(44.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Transaktion 62108127-0002"), //
                        hasAmount("CHF", 251.86), hasGrossValue("CHF", 250.23), //
                        hasTaxes("CHF", 0.38), hasFees("CHF", 1.25))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
                        hasIsin("IE00BKS7L097"), hasWkn("51992937"), hasTicker(null), //
                        hasName("Inv S&P 500 ESG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-04T00:00"), hasShares(4.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Transaktion 62108136-0002"), //
                        hasAmount("CHF", 260.10), hasGrossValue("CHF", 258.42), //
                        hasTaxes("CHF", 0.39), hasFees("CHF", 1.29))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
                        hasIsin("IE00B3RBWM25"), hasWkn("18575459"), hasTicker(null), //
                        hasName("Van FTSE All Wr"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-03T00:00"), hasShares(8.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Transaktion 52584421-1234"), //
                        hasAmount("CHF", 929.78), hasGrossValue("CHF", 923.77), //
                        hasTaxes("CHF", 1.39), hasFees("CHF", 4.62))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
                        hasIsin("IE00B6YX5D40"), hasWkn("13976063"), hasTicker(null), //
                        hasName("SPDR S&P US Di"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-03T00:00"), hasShares(7.00), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Transaktion 85741290-0003"), //
                        hasAmount("CHF", 461.87), hasGrossValue("CHF", 458.89), //
                        hasTaxes("CHF", 0.69), hasFees("CHF", 2.29))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
                        hasIsin("IE000V93BNU0"), hasWkn("113303241"), hasTicker(null), //
                        hasName("Wld ESG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-03T00:00"), hasShares(113.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Transaktion 12301780-0324"), //
                        hasAmount("CHF", 519.20), hasGrossValue("CHF", 515.85), //
                        hasTaxes("CHF", 0.77), hasFees("CHF", 2.58))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
                        hasIsin("US5949181045"), hasWkn("951692"), hasTicker(null), //
                        hasName("Registered Shs Microsoft Corp"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-09T00:00"), hasShares(15.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Transaktion 69155585-0002"), //
                        hasAmount("CHF", 5399.37), hasGrossValue("CHF", 5337.98), //
                        hasTaxes("CHF", 8.01), hasFees("CHF", 53.38))));
    }

    @Test
    public void testDividende01()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
                        hasIsin("IE00B3RBWM25"), hasWkn("18575459"), hasTicker(null), //
                        hasName("Van FTSE All Wr"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-26T00:00"), hasShares(168.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Quartalsdividende | Transaktion 65062408"), //
                        hasAmount("CHF", 116.96), hasGrossValue("CHF", 116.96), //
                        hasForexGrossValue("USD", 132.53), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInCHF()
    {
        Security security = new Security("Van FTSE All Wr", "CHF");
        security.setIsin("IE00B3RBWM25");
        security.setWkn("18575459");

        Client client = new Client();
        client.addSecurity(security);

        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-26T00:00"), hasShares(168.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Quartalsdividende | Transaktion 65062408"), //
                        hasAmount("CHF", 116.96), hasGrossValue("CHF", 116.96), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode("CHF");
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende02()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
                        hasIsin("IE00B6YX5D40"), hasWkn("13976063"), hasTicker(null), //
                        hasName("SPDR S&P US Di"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-02T00:00"), hasShares(7.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Quartalsdividende | Transaktion 65379273"), //
                        hasAmount("CHF", 2.66), hasGrossValue("CHF", 2.66), //
                        hasForexGrossValue("USD", 2.99), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInCHF()
    {
        Security security = new Security("SPDR S&P US Di", "CHF");
        security.setIsin("IE00B6YX5D40");
        security.setWkn("13976063");

        Client client = new Client();
        client.addSecurity(security);

        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-02T00:00"), hasShares(7.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Quartalsdividende | Transaktion 65379273"), //
                        hasAmount("CHF", 2.66), hasGrossValue("CHF", 2.66), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode("CHF");
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende03()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0008742519"), hasWkn("874251"), hasTicker(null), //
                        hasName("Namen-Akt Swisscom AG Nom."), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-04-04T00:00"), hasShares(10.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Jahresdividende | Transaktion 46148061"), //
                        hasAmount("CHF", 143.00), hasGrossValue("CHF", 220.00), //
                        hasTaxes("CHF", 77.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0008837566"), hasWkn("883756"), hasTicker(null), //
                        hasName("Namen-Akt Allreal Holding AG Nom."), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-04-25T00:00"), hasShares(25.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Jahresdividende | Transaktion 57923179"), //
                        hasAmount("CHF", 56.87), hasGrossValue("CHF", 87.50), //
                        hasTaxes("CHF", 30.63), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        HypothekarbankLenzburgAGPDFExtractor extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0008837566"), hasWkn("883756"), hasTicker(null), //
                        hasName("Namen-Akt Allreal Holding AG Nom."), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-04-25T00:00"), hasShares(25.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Transaktion 62847906"), //
                        hasAmount("CHF", 87.50), hasGrossValue("CHF", 87.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }
}
