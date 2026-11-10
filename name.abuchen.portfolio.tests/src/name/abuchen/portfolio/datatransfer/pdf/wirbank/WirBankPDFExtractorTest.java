package name.abuchen.portfolio.datatransfer.pdf.wirbank;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
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
import name.abuchen.portfolio.datatransfer.pdf.WirBankPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class WirBankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B5BMR087"), hasWkn(null), hasTicker(null), //
                        hasName("iShares Core S&P500"), //
                        hasCurrencyCode("USD"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-07-05T00:00"), hasShares(1.369), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 360.43), hasGrossValue("CHF", 359.97), //
                        hasForexGrossValue("USD", 359.27), //
                        hasTaxes("CHF", 0.46), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf01WithSecurityInCHF()
    {
        var security = new Security("iShares Core S&P500", "CHF");
        security.setIsin("IE00B5BMR087");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

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
                        hasDate("2018-07-05T00:00"), hasShares(1.369), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 360.43), hasGrossValue("CHF", 359.97), //
                        hasTaxes("CHF", 0.46), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0110869143"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF SPI Extra"), //
                        hasCurrencyCode("CHF"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-07-04T00:00"), hasShares(0.051), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 104.78), hasGrossValue("CHF", 104.78), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B5BMR087"), hasWkn(null), hasTicker(null), //
                        hasName("iShares Core S&P500"), //
                        hasCurrencyCode("USD"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-01-07T00:00"), hasShares(1.924), //
                        hasSource("Kauf03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1001.08), hasGrossValue("CHF", 999.40), //
                        hasForexGrossValue("USD", 1001.02), //
                        hasTaxes("CHF", 1.68), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInCHF()
    {
        var security = new Security("iShares Core S&P500", "CHF");
        security.setIsin("IE00B5BMR087");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

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
                        hasDate("2019-01-07T00:00"), hasShares(1.924), //
                        hasSource("Kauf03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1001.08), hasGrossValue("CHF", 999.40), //
                        hasTaxes("CHF", 1.68), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0033782431"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF SMI"), //
                        hasCurrencyCode("CHF"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-01-07T00:00"), hasShares(1.766), //
                        hasSource("Kauf04.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1886.94), hasGrossValue("CHF", 1886.94), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0037606552"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Europe ex CH"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-01-07T00:00"), hasShares(1.024), //
                        hasSource("Kauf05.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 719.24), hasGrossValue("CHF", 719.24), //
                        hasForexGrossValue("EUR", 635.26), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf05WithSecurityInCHF()
    {
        var security = new Security("CSIF Europe ex CH", "CHF");
        security.setIsin("CH0037606552");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

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
                        hasDate("2019-01-07T00:00"), hasShares(1.024), //
                        hasSource("Kauf05.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 719.24), hasGrossValue("CHF", 719.24), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06_English.txt"), errors);

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
                        hasIsin("LU0629460675"), hasWkn(null), hasTicker(null), //
                        hasName("UBS ETF MSCI EMU SRI"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-03T00:00"), hasShares(0.114), //
                        hasSource("Kauf06_English.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 11.30), hasGrossValue("CHF", 11.29), //
                        hasForexGrossValue("EUR", 10.58), //
                        hasTaxes("CHF", 0.01), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf06WithSecurityInCHF()
    {
        var security = new Security("UBS ETF MSCI EMU SRI", "CHF");
        security.setIsin("LU0629460675");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06_English.txt"), errors);

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
                        hasDate("2020-07-03T00:00"), hasShares(0.114), //
                        hasSource("Kauf06_English.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 11.30), hasGrossValue("CHF", 11.29), //
                        hasTaxes("CHF", 0.01), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
                        hasIsin("CH0037606552"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Europe ex CH"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-19T00:00"), hasShares(0.477), //
                        hasSource("Kauf07.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 421.47), hasGrossValue("CHF", 421.47), //
                        hasForexGrossValue("EUR", 402.34), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf07WithSecurityInCHF()
    {
        var security = new Security("CSIF Europe ex CH", "CHF");
        security.setIsin("CH0037606552");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

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
                        hasDate("2022-05-19T00:00"), hasShares(0.477), //
                        hasSource("Kauf07.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 421.47), hasGrossValue("CHF", 421.47), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0037606552"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Europe ex CH"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-31T00:00"), hasShares(0.025), //
                        hasSource("Kauf08.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 21.67), hasGrossValue("CHF", 21.67), //
                        hasForexGrossValue("EUR", 22.53), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf08WithSecurityInCHF()
    {
        var security = new Security("CSIF Europe ex CH", "CHF");
        security.setIsin("CH0037606552");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-31T00:00"), hasShares(0.025), //
                        hasSource("Kauf08.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 21.67), hasGrossValue("CHF", 21.67), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0031419960"), hasWkn(null), hasTicker(null), //
                        hasName("CSIMF Money Market CHF"), //
                        hasCurrencyCode("CHF"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-06T00:00"), hasShares(0.001), //
                        hasSource("Kauf09.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 0.44), hasGrossValue("CHF", 0.44), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10_English.txt"), errors);

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
                        hasIsin("CH0117044831"), hasWkn(null), hasTicker(null), //
                        hasName("Swisscanto Pacific ex Japan"), //
                        hasCurrencyCode("USD"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-08T00:00"), hasShares(0.027), //
                        hasSource("Kauf10_English.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 3.16), hasGrossValue("CHF", 3.16), //
                        hasForexGrossValue("USD", 3.54), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf10WithSecurityInCHF()
    {
        var security = new Security("Swisscanto Pacific ex Japan", "CHF");
        security.setIsin("CH0117044831");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10_English.txt"), errors);

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
                        hasDate("2023-09-08T00:00"), hasShares(0.027), //
                        hasSource("Kauf10_English.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 3.16), hasGrossValue("CHF", 3.16), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11_Francais.txt"), errors);

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
                        hasIsin("CH0030849613"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Canada"), //
                        hasCurrencyCode("CAD"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-19T00:00"), hasShares(0.027), //
                        hasSource("Kauf11_Francais.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 32.84), hasGrossValue("CHF", 32.84), //
                        hasForexGrossValue("CAD", 42.12), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf11WithSecurityInCHF()
    {
        var security = new Security("CSIF Canada", "CHF");
        security.setIsin("CH0030849613");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11_Francais.txt"), errors);

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
                        hasDate("2022-05-19T00:00"), hasShares(0.027), //
                        hasSource("Kauf11_Francais.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 32.84), hasGrossValue("CHF", 32.84), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf12_ViacInvest.txt"), errors);

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
                        hasIsin("CH1336969030"), hasWkn(null), hasTicker(null), //
                        hasName("VIAC Equity North America Sustainable"), //
                        hasCurrencyCode("CHF"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-09T00:00"), hasShares(16.839), //
                        hasSource("Kauf12_ViacInvest.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1709.70), hasGrossValue("CHF", 1709.70), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf13_ViacInvest_English.txt"), errors);

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
                        hasIsin("CH1336969097"), hasWkn(null), hasTicker(null), //
                        hasName("VIAC Equity Switzerland Sustainable"), //
                        hasCurrencyCode("CHF"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-16T00:00"), hasShares(0.682), //
                        hasSource("Kauf13_ViacInvest_English.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 67.72), hasGrossValue("CHF", 67.72), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testInterest01()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zins01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2019-03-31T00:00"), //
                        hasSource("Zins01.txt"), //
                        hasNote("Zinssatz: 0.11% | Zinsperiode: März"), //
                        hasAmount("CHF", 0.04), hasGrossValue("CHF", 0.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testInterest02()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zins02_English.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-06-30T00:00"), //
                        hasSource("Zins02_English.txt"), //
                        hasNote("Interest rate: 0.10% | Interest period: June"), //
                        hasAmount("CHF", 0.05), hasGrossValue("CHF", 0.05), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testInterest03()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zins03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2021-01-31T00:00"), //
                        hasSource("Zins03.txt"), //
                        hasNote("Zinssatz: 0.05% | Zinsperiode: Januar"), //
                        hasAmount("CHF", 0.19), hasGrossValue("CHF", 0.19), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testInterest04()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zins04_Francais.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-05-31T00:00"), //
                        hasSource("Zins04_Francais.txt"), //
                        hasNote("Taux d’intérêt: 0.10% | Période d’intérêt: mai"), //
                        hasAmount("CHF", 0.04), hasGrossValue("CHF", 0.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testInterest05()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zins05_English.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-10-31T00:00"), //
                        hasSource("Zins05_English.txt"), //
                        hasNote("Interest rate: 0.90% | Interest period: October"), //
                        hasAmount("CHF", 0.30), hasGrossValue("CHF", 0.30), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testInterest06()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Zins06_Francais_2019.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2019-11-30T00:00"), //
                        hasSource("Zins06_Francais_2019.txt"), //
                        hasNote("Taux d’intérêt: 0.20% | Période d’intérêt: novembre"), //
                        hasAmount("CHF", 1.15), hasGrossValue("CHF", 1.15), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testFees01()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Gebuehren01.txt"), errors);

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
                        hasDate("2019-01-31T00:00"), //
                        hasSource("Gebuehren01.txt"), //
                        hasNote("VIAC Verwaltungsgebühr: 0.123%"), //
                        hasAmount("CHF", 1.11), hasGrossValue("CHF", 1.11), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testFees02()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Gebuehren02_English.txt"), errors);

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
                        hasDate("2020-06-30T00:00"), //
                        hasSource("Gebuehren02_English.txt"), //
                        hasNote("VIAC administration fee: 0.47%"), //
                        hasAmount("CHF", 4.23), hasGrossValue("CHF", 4.23), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testFees03()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Gebuehren03_English.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        fee( //
                                        hasDate("2023-07-31T00:00"), //
                                        hasSource("Gebuehren03_English.txt"), //
                                        hasNote("VIAC administration fee: 0.00%"), //
                                        hasAmount("CHF", 0.00), hasGrossValue("CHF", 0.00), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));
    }

    @Test
    public void testFees04()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Gebuehren04_English.txt"), errors);

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
                        hasDate("2023-10-31T00:00"), //
                        hasSource("Gebuehren04_English.txt"), //
                        hasNote("VIAC administration fee: 0.24%"), //
                        hasAmount("CHF", 5.45), hasGrossValue("CHF", 5.45), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testFees05()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Gebuehren05_Francais.txt"), errors);

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
                        hasDate("2019-04-30T00:00"), //
                        hasSource("Gebuehren05_Francais.txt"), //
                        hasNote("Commission de gestion effective VIAC: 0.26%"), //
                        hasAmount("CHF", 3.10), hasGrossValue("CHF", 3.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testCreditNote01()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CreditNote01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-01-15T00:00"), hasAmount("CHF", 2150.00), //
                        hasSource("CreditNote01.txt"), hasNote("Einzahlung ABCD"))));
    }

    @Test
    public void testCreditNote02()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CreditNote02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-01-15T00:00"), hasAmount("CHF", 150.00), //
                        hasSource("CreditNote02.txt"), hasNote("Einzahlung ABCD"))));
    }

    @Test
    public void testCreditNote03()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CreditNote03_English.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-12-17T00:00"), hasAmount("CHF", 1000.00), //
                        hasSource("CreditNote03_English.txt"), hasNote("Deposit BESR"))));
    }

    @Test
    public void testCreditNote04()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CreditNote04_Francais.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-12-27T00:00"), hasAmount("CHF", 6768.00), //
                        hasSource("CreditNote04_Francais.txt"), hasNote("Versement BVRB"))));
    }

    @Test
    public void testCreditNote05()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CreditNote05_ViacInvest_English.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-08T00:00"), hasAmount("CHF", 610.00), //
                        hasSource("CreditNote05_ViacInvest_English.txt"), hasNote("John Doe"))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0030849654"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Pacific ex Japan"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-05-21T00:00"), hasExDate(null), //
                        hasShares(0.242), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Ordentliche Dividende"), //
                        hasAmount("CHF", 0.04), hasGrossValue("CHF", 0.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0030849613"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Canada"), //
                        hasCurrencyCode("CAD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-05-21T00:00"), hasExDate(null), //
                        hasShares(0.176), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Ordentliche Dividende"), //
                        hasAmount("CHF", 0.15), hasGrossValue("CHF", 0.15), //
                        hasForexGrossValue("CAD", 0.20), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInCHF()
    {
        var security = new Security("CSIF Canada", "CHF");
        security.setIsin("CH0030849613");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-05-21T00:00"), hasExDate(null), //
                        hasShares(0.176), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Ordentliche Dividende"), //
                        hasAmount("CHF", 0.15), hasGrossValue("CHF", 0.15), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03_English.txt"), errors);

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
                        hasIsin("IE00B1FZSF77"), hasWkn(null), hasTicker(null), //
                        hasName("iShares US Property Yield"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-02-27T00:00"), hasExDate(null), //
                        hasShares(2.142), //
                        hasSource("Dividende03_English.txt"), //
                        hasNote("Ordinary dividend"), //
                        hasAmount("CHF", 0.52), hasGrossValue("CHF", 0.52), //
                        hasForexGrossValue("USD", 0.54), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInCHF()
    {
        var security = new Security("iShares US Property Yield", "CHF");
        security.setIsin("IE00B1FZSF77");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03_English.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-02-27T00:00"), hasExDate(null), //
                        hasShares(2.142), //
                        hasSource("Dividende03_English.txt"), //
                        hasNote("Ordinary dividend"), //
                        hasAmount("CHF", 0.52), hasGrossValue("CHF", 0.52), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0629460089"), hasWkn(null), hasTicker(null), //
                        hasName("UBS ETF MSCI USA SRI"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-02-04T00:00"), hasExDate(null), //
                        hasShares(47.817), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Ordentliche Dividende"), //
                        hasAmount("CHF", 31.44), hasGrossValue("CHF", 31.44), //
                        hasForexGrossValue("USD", 34.26), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInCHF()
    {
        var security = new Security("UBS ETF MSCI USA SRI", "CHF");
        security.setIsin("LU0629460089");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-02-04T00:00"), hasExDate(null), //
                        hasShares(47.817), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Ordentliche Dividende"), //
                        hasAmount("CHF", 31.44), hasGrossValue("CHF", 31.44), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0032912732"), hasWkn(null), hasTicker(null), //
                        hasName("UBS ETF SLI"), //
                        hasCurrencyCode("CHF"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-14T00:00"), hasExDate(null), //
                        hasShares(31.64), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Ordentliche Dividende"), //
                        hasAmount("CHF", 15.19), hasGrossValue("CHF", 15.19), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06_Francais.txt"), errors);

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
                        hasIsin("CH0030849613"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Canada"), //
                        hasCurrencyCode("CAD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-24T00:00"), hasExDate(null), //
                        hasShares(0.027), //
                        hasSource("Dividende06_Francais.txt"), //
                        hasNote("Dividende ordinaire"), //
                        hasAmount("CHF", 0.02), hasGrossValue("CHF", 0.02), //
                        hasForexGrossValue("CAD", 0.03), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende06WithSecurityInCHF()
    {
        var security = new Security("CSIF Canada", "CHF");
        security.setIsin("CH0030849613");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06_Francais.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-24T00:00"), hasExDate(null), //
                        hasShares(0.027), //
                        hasSource("Dividende06_Francais.txt"), //
                        hasNote("Dividende ordinaire"), //
                        hasAmount("CHF", 0.02), hasGrossValue("CHF", 0.02), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0030849712"), hasWkn(null), hasTicker(null), //
                        hasName("UBS US - Pension Fund"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-27T00:00"), hasExDate(null), //
                        hasShares(2.128), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Ordentliche Dividende"), //
                        hasAmount("CHF", 1.52), hasGrossValue("CHF", 1.52), //
                        hasForexGrossValue("USD", 1.85), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende07WithSecurityInCHF()
    {
        var security = new Security("UBS US - Pension Fund", "CHF");
        security.setIsin("CH0030849712");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-27T00:00"), hasExDate(null), //
                        hasShares(2.128), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Ordentliche Dividende"), //
                        hasAmount("CHF", 1.52), hasGrossValue("CHF", 1.52), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende07MitSteuerrueckerstattung10()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende07.txt", "Steuerrueckerstattung10.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0030849712"), hasWkn(null), hasTicker(null), //
                        hasName("UBS US - Pension Fund"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-27T00:00"), hasExDate(null), //
                        hasShares(2.128), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Ordentliche Dividende"), //
                        hasAmount("CHF", 1.52), hasGrossValue("CHF", 1.52), //
                        hasForexGrossValue("USD", 1.85), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-05-27T00:00"), hasShares(2.128), //
                        hasSource("Steuerrueckerstattung10.txt"), //
                        hasNote("Rückerstattung Quellensteuer"), //
                        hasAmount("CHF", 0.82), hasGrossValue("CHF", 0.82), //
                        hasForexGrossValue("USD", 1.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSteuerrueckerstattung10MitDividende07()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung10.txt", "Dividende07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0030849712"), hasWkn(null), hasTicker(null), //
                        hasName("UBS US - Pension Fund"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-27T00:00"), hasExDate(null), //
                        hasShares(2.128), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Ordentliche Dividende"), //
                        hasAmount("CHF", 1.52), hasGrossValue("CHF", 1.52), //
                        hasForexGrossValue("USD", 1.85), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-05-27T00:00"), hasShares(2.128), //
                        hasSource("Steuerrueckerstattung10.txt"), //
                        hasNote("Rückerstattung Quellensteuer"), //
                        hasAmount("CHF", 0.82), hasGrossValue("CHF", 0.82), //
                        hasForexGrossValue("USD", 1.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende07MitSteuerrueckerstattung10WithSecurityInUSD()
    {
        var security = new Security("UBS US - Pension Fund", "USD");
        security.setIsin("CH0030849712");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Dividende07.txt", "Steuerrueckerstattung10.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-27T00:00"), hasExDate(null), //
                        hasShares(2.128), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Ordentliche Dividende"), //
                        hasAmount("CHF", 1.52), hasGrossValue("CHF", 1.52), //
                        hasForexGrossValue("USD", 1.85), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-05-27T00:00"), hasShares(2.128), //
                        hasSource("Steuerrueckerstattung10.txt"), //
                        hasNote("Rückerstattung Quellensteuer"), //
                        hasAmount("CHF", 0.82), hasGrossValue("CHF", 0.82), //
                        hasForexGrossValue("USD", 1.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08_English.txt"), errors);

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
                        hasIsin("CH0030849613"), hasWkn(null), hasTicker(null), //
                        hasName("UBS Canada"), //
                        hasCurrencyCode("CAD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-05-28T00:00"), hasExDate(null), //
                        hasShares(0.196), //
                        hasSource("Dividende08_English.txt"), //
                        hasNote("Ordinary dividend"), //
                        hasAmount("CHF", 0.09), hasGrossValue("CHF", 0.09), //
                        hasForexGrossValue("CAD", 0.15), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende08WithSecurityInCHF()
    {
        var security = new Security("UBS Canada", "CHF");
        security.setIsin("CH0030849613");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08_English.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-05-28T00:00"), hasExDate(null), //
                        hasShares(0.196), //
                        hasSource("Dividende08_English.txt"), //
                        hasNote("Ordinary dividend"), //
                        hasAmount("CHF", 0.09), hasGrossValue("CHF", 0.09), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende08MitSteuerrueckerstattung09()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), //
                        "Dividende08_English.txt", //
                        "Steuerrueckerstattung09_English.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0030849613"), hasWkn(null), hasTicker(null), //
                        hasName("UBS Canada"), //
                        hasCurrencyCode("CAD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-05-28T00:00"), hasExDate(null), //
                        hasShares(0.196), //
                        hasSource("Dividende08_English.txt"), //
                        hasNote("Ordinary dividend"), //
                        hasAmount("CHF", 0.09), hasGrossValue("CHF", 0.09), //
                        hasForexGrossValue("CAD", 0.15), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-05-28T00:00"), hasShares(0.196), //
                        hasSource("Steuerrueckerstattung09_English.txt"), //
                        hasNote("Refund Swiss withholding tax"), //
                        hasAmount("CHF", 0.05), hasGrossValue("CHF", 0.05), //
                        hasForexGrossValue("CAD", 0.09), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CHF");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0032400670"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF World ex CH"), //
                        hasCurrencyCode("USD"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-12-05T00:00"), hasShares(0.03), //
                        hasSource("Verkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 45.66), hasGrossValue("CHF", 45.66), //
                        hasForexGrossValue("USD", 45.71), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01WithSecurityInCHF()
    {
        var security = new Security("CSIF World ex CH", "CHF");
        security.setIsin("CH0032400670");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-12-05T00:00"), hasShares(0.03), //
                        hasSource("Verkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 45.66), hasGrossValue("CHF", 45.66), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02_English.txt"), errors);

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
                        hasIsin("CH0033782431"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF SMI"), //
                        hasCurrencyCode("CHF"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-02-05T00:00"), hasShares(0.035), //
                        hasSource("Verkauf02_English.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 48.24), hasGrossValue("CHF", 48.24), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0209106761"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Gold"), //
                        hasCurrencyCode("USD"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-09T00:00"), hasShares(0.135), //
                        hasSource("Verkauf03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 200.69), hasGrossValue("CHF", 200.69), //
                        hasForexGrossValue("USD", 205.76), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03WithSecurityInCHF()
    {
        var security = new Security("CSIF Gold", "CHF");
        security.setIsin("CH0209106761");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-06-09T00:00"), hasShares(0.135), //
                        hasSource("Verkauf03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 200.69), hasGrossValue("CHF", 200.69), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH1220910934"), hasWkn(null), hasTicker(null), //
                        hasName("Swisscanto World ex CH Small Cap Responsible - IPF"), //
                        hasCurrencyCode("USD"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-08T00:00"), hasShares(0.148), //
                        hasSource("Verkauf04.txt"), hasNote(null), //
                        hasAmount("CHF", 14.41), hasGrossValue("CHF", 14.41), //
                        hasForexGrossValue("USD", 16.30), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf04WithSecurityInCHF()
    {
        var security = new Security("Swisscanto World ex CH Small Cap Responsible - IPF", "CHF");
        security.setIsin("CH1220910934");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-08T00:00"), hasShares(0.148), //
                        hasSource("Verkauf04.txt"), hasNote(null), //
                        hasAmount("CHF", 14.41), hasGrossValue("CHF", 14.41), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05_ViacInvest.txt"), errors);

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
                        hasIsin("CH1336969030"), hasWkn(null), hasTicker(null), //
                        hasName("VIAC Equity North America Sustainable"), //
                        hasCurrencyCode("CHF"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-01-16T00:00"), hasShares(0.181), //
                        hasSource("Verkauf05_ViacInvest.txt"), hasNote(null), //
                        hasAmount("CHF", 18.10), hasGrossValue("CHF", 18.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSteuerrueckerstattung01()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung01.txt"), errors);

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
                        hasIsin("CH0030849654"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Pacific ex Japan"), //
                        hasCurrencyCode("CHF"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2022-05-23T00:00"), hasShares(0.966), //
                        hasSource("Steuerrueckerstattung01.txt"), //
                        hasNote("Rückerstattung Quellensteuer"), //
                        hasAmount("CHF", 11.59), hasGrossValue("CHF", 11.59), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSteuerrueckerstattung02()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung02.txt"), errors);

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
                        hasIsin("CH0017844686"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Emerging Markets"), //
                        hasCurrencyCode("CHF"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2020-02-27T00:00"), hasShares(0.353), //
                        hasSource("Steuerrueckerstattung02.txt"), //
                        hasNote("Rückerstattung Quellensteuer"), //
                        hasAmount("CHF", 5.73), hasGrossValue("CHF", 5.73), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSteuerrueckerstattung03()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung03.txt"), errors);

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
                        hasIsin("CH0037606552"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Europe ex CH"), //
                        hasCurrencyCode("CHF"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2020-02-27T00:00"), hasShares(0.154), //
                        hasSource("Steuerrueckerstattung03.txt"), //
                        hasNote("Rückerstattung Quellensteuer"), //
                        hasAmount("CHF", 1.36), hasGrossValue("CHF", 1.36), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSteuerrueckerstattung03WithSecurityInEUR()
    {
        var security = new Security("CSIF Europe ex CH", "EUR");
        security.setIsin("CH0037606552");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        taxRefund( //
                                        hasDate("2020-02-27T00:00"), hasShares(0.154), //
                                        hasSource("Steuerrueckerstattung03.txt"), //
                                        hasNote("Rückerstattung Quellensteuer"), //
                                        hasAmount("CHF", 1.36), hasGrossValue("CHF", 1.36), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));
    }

    @Test
    public void testSteuerrueckerstattung03MitKauf05()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung03.txt", "Kauf05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0037606552"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Europe ex CH"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-01-07T00:00"), hasShares(1.024), //
                        hasSource("Kauf05.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 719.24), hasGrossValue("CHF", 719.24), //
                        hasForexGrossValue("EUR", 635.26), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        taxRefund( //
                                        hasDate("2020-02-27T00:00"), hasShares(0.154), //
                                        hasSource("Steuerrueckerstattung03.txt"), //
                                        hasNote("Rückerstattung Quellensteuer"), //
                                        hasAmount("CHF", 1.36), hasGrossValue("CHF", 1.36), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));
    }

    @Test
    public void testSteuerrueckerstattung04()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung04_English.txt"),
                        errors);

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
                        hasIsin("CH0037606552"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Europe ex CH"), //
                        hasCurrencyCode("CHF"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-02-25T00:00"), hasShares(1.841), //
                        hasSource("Steuerrueckerstattung04_English.txt"), //
                        hasNote("Refund withholding tax"), //
                        hasAmount("CHF", 17.28), hasGrossValue("CHF", 17.28), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSteuerrueckerstattung05()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung05_English.txt"),
                        errors);

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
                        hasIsin("CH0017844686"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Emerging Markets"), //
                        hasCurrencyCode("CHF"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2022-05-23T00:00"), hasShares(0.104), //
                        hasSource("Steuerrueckerstattung05_English.txt"), //
                        hasNote("Refund withholding tax"), //
                        hasAmount("CHF", 1.65), hasGrossValue("CHF", 1.65), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSteuerrueckerstattung06()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung06_English.txt"),
                        errors);

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
                        hasIsin("CH0489405321"), hasWkn(null), hasTicker(null), //
                        hasName("Swisscanto Japan - IPF"), //
                        hasCurrencyCode("CHF"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-08-25T00:00"), hasShares(0.423), //
                        hasSource("Steuerrueckerstattung06_English.txt"), //
                        hasNote("Refund withholding tax"), //
                        hasAmount("CHF", 0.34), hasGrossValue("CHF", 0.34), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSteuerrueckerstattung07()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung07.txt"), errors);

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
                        hasIsin("CH0030849613"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Canada"), //
                        hasCurrencyCode("CHF"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-04-19T00:00"), hasShares(0.431), //
                        hasSource("Steuerrueckerstattung07.txt"), //
                        hasNote("Rückerstattung Quellensteuer"), //
                        hasAmount("CHF", 0.05), hasGrossValue("CHF", 0.05), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSteuerrueckerstattung07WithSecurityInCAD()
    {
        var security = new Security("CSIF Canada", "CAD");
        security.setIsin("CH0030849613");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        taxRefund( //
                                        hasDate("2024-04-19T00:00"), hasShares(0.431), //
                                        hasSource("Steuerrueckerstattung07.txt"), //
                                        hasNote("Rückerstattung Quellensteuer"), //
                                        hasAmount("CHF", 0.05), hasGrossValue("CHF", 0.05), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));
    }

    @Test
    public void testSteuerrueckerstattung08()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung08.txt"), errors);

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
                        hasIsin("CH1390275134"), hasWkn(null), hasTicker(null), //
                        hasName("UBS Emerging Markets"), //
                        hasCurrencyCode("CHF"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-05-29T00:00"), hasShares(1.034), //
                        hasSource("Steuerrueckerstattung08.txt"), //
                        hasNote("Rückerstattung Verrechnungssteuer"), //
                        hasAmount("CHF", 17.36), hasGrossValue("CHF", 17.36), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSteuerrueckerstattung09()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung09_English.txt"),
                        errors);

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
                        hasIsin("CH0030849613"), hasWkn(null), hasTicker(null), //
                        hasName("UBS Canada"), //
                        hasCurrencyCode("CHF"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-05-28T00:00"), hasShares(0.196), //
                        hasSource("Steuerrueckerstattung09_English.txt"), //
                        hasNote("Refund Swiss withholding tax"), //
                        hasAmount("CHF", 0.05), hasGrossValue("CHF", 0.05), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSteuerrueckerstattung10()
    {
        var extractor = new WirBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung10.txt"), errors);

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
                        hasIsin("CH0030849712"), hasWkn(null), hasTicker(null), //
                        hasName("UBS US - Pension Fund"), //
                        hasCurrencyCode("CHF"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-05-27T00:00"), hasShares(2.128), //
                        hasSource("Steuerrueckerstattung10.txt"), //
                        hasNote("Rückerstattung Quellensteuer"), //
                        hasAmount("CHF", 0.82), hasGrossValue("CHF", 0.82), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSteuerrueckerstattung10WithSecurityInUSD()
    {
        var security = new Security("UBS US - Pension Fund", "USD");
        security.setIsin("CH0030849712");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new WirBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerrueckerstattung10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        taxRefund( //
                                        hasDate("2025-05-27T00:00"), hasShares(2.128), //
                                        hasSource("Steuerrueckerstattung10.txt"), //
                                        hasNote("Rückerstattung Quellensteuer"), //
                                        hasAmount("CHF", 0.82), hasGrossValue("CHF", 0.82), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));
    }

    @Test
    public void testDividendeStorno01()
    {
        var extractor = new WirBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0030849654"), hasWkn(null), hasTicker(null), //
                        hasName("CSIF Pacific ex Japan"), //
                        hasCurrencyCode("CHF"))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        taxRefund( //
                                        hasDate("2022-05-23T00:00"), hasShares(0.515), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote("Refund withholding tax"), //
                                        hasAmount("CHF", 6.18), hasGrossValue("CHF", 6.18), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));
    }
}
