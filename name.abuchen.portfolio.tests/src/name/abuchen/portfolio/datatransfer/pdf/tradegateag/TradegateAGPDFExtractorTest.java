package name.abuchen.portfolio.datatransfer.pdf.tradegateag;

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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TradegateAGPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class TradegateAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        TradegateAGPDFExtractor extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BM67HN09"), hasWkn("A113FG"), hasTicker(null), //
                        hasName("Xtr.(IE)-MSCI Wrld Con.Staples Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-04T11:04:53"), hasShares(3.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Order-/Ref.nr. 9876543 | Limit 43,2500 EUR"), //
                        hasAmount("EUR", 129.75), hasGrossValue("EUR", 129.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        TradegateAGPDFExtractor extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BM67HN09"), hasWkn("A113FG"), hasTicker(null), //
                        hasName("Xtr.(IE)-MSCI Wrld Con.Staples Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-06-05T09:22:27"), hasShares(3.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Order-/Ref.nr. 9876543 | Limit 43,6600 EUR"), //
                        hasAmount("EUR", 130.75), hasGrossValue("EUR", 130.98), //
                        hasTaxes("EUR", 0.22 + 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        TradegateAGPDFExtractor extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BVZ6SP04"), hasWkn("A14PHG"), hasTicker(null), //
                        hasName("PFI ETFs-EO Sh.Mat.UC.ETF Registered Shares EUR Acc.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-11-13T20:33:19"), hasShares(10.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Order-/Ref.nr. 8321468 | Limit 102,8600 EUR"), //
                        hasAmount("EUR", 1028.40), hasGrossValue("EUR", 1028.67), //
                        hasTaxes("EUR", 0.24 + 0.01 + 0.02), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        TradegateAGPDFExtractor extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BZ163G84"), hasWkn("A143JK"), hasTicker(null), //
                        hasName("Vanguard EUR Corp.Bond U.ETF Registered Shares EUR Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-26T00:00"), hasShares(76.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Order-/Ref.nr. 9876543"), //
                        hasAmount("EUR", 7.16), hasGrossValue("EUR", 9.72), //
                        hasTaxes("EUR", 2.43 + 0.13), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        TradegateAGPDFExtractor extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BZ163L38"), hasWkn("A143JQ"), hasTicker(null), //
                        hasName("Vang.USD Em.Mkts Gov.Bd U.ETF Registered Shares USD Dis.oN"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-09-25T00:00"), hasShares(59.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Order-/Ref.nr. 9876543"), //
                        hasAmount("EUR", 7.21), hasGrossValue("EUR", 9.79), //
                        hasForexGrossValue("USD", 10.96), //
                        hasTaxes("EUR", (2.74 + 0.15) / 1.11856), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        Security security = new Security("Vang.USD Em.Mkts Gov.Bd U.ETF Registered Shares USD Dis.oN", CurrencyUnit.EUR);
        security.setIsin("IE00BZ163L38");
        security.setWkn("A143JQ");

        Client client = new Client();
        client.addSecurity(security);

        TradegateAGPDFExtractor extractor = new TradegateAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-09-25T00:00"), hasShares(59.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Order-/Ref.nr. 9876543"), //
                        hasAmount("EUR", 7.21), hasGrossValue("EUR", 9.79), //
                        hasTaxes("EUR", (2.74 + 0.15) / 1.11856), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }
}
