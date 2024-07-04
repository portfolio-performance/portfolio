package name.abuchen.portfolio.datatransfer.pdf.raisinbankag;

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
import name.abuchen.portfolio.datatransfer.pdf.RaisinBankAGPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class RaisinBankAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        RaisinBankAGPDFExtractor extractor = new RaisinBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B9F5YL18"), hasWkn(null), hasTicker(null), //
                        hasName("VANG.FTSE D.A.P.X.J.DLD"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-23T12:10:43"), hasShares(0.18854), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Abrechnungsnummer: 95a8535f-0aa7-4197-85ae-3f7337d9232d"), //
                        hasAmount("EUR", 4.54), hasGrossValue("EUR", 4.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        RaisinBankAGPDFExtractor extractor = new RaisinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B42W4L06"), hasWkn(null), hasTicker(null), //
                        hasName("VAN.INVT-GL SC IDX EOA"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-24T00:00"), hasShares(0.03178), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Abrechnungsnummer: acc8e2cf-f87f-4372-8b11-8cd3eea151ce"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        RaisinBankAGPDFExtractor extractor = new RaisinBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00BKX55R35"), hasWkn(null), hasTicker(null), //
                        hasName("VANG.FTSE N.AME.U.ETF DLD"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-02T12:11:16"), hasShares(0.0075), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Abrechnungsnummer: bf7448fe-2a12-4c8e-bc98-e6b7411fb56b"), //
                        hasAmount("EUR", 0.88), hasGrossValue("EUR", 0.92), //
                        hasTaxes("EUR", 0.04), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        RaisinBankAGPDFExtractor extractor = new RaisinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B945VV12"), hasWkn(null), hasTicker(null), //
                        hasName("VANG.FTSE DEV.EU.UETF EOD"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-26T00:00"), hasShares(154.9996), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Auftragsnummer: 680ZY015-5cfb-4240-961c-865127t5147j"), //
                        hasAmount("EUR", 92.21), hasGrossValue("EUR", 114.67), //
                        hasTaxes("EUR", 19.63 + 1.07 + 1.76), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        RaisinBankAGPDFExtractor extractor = new RaisinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3VVMM84"), hasWkn(null), hasTicker(null), //
                        hasName("VANGUARD FTSE EMU.ETF DLD"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-01T00:00"), hasShares(6.38096), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Auftragsnummer: 237dcf5e-bbe9-4127-9eef-4153d9c85ccf"), //
                        hasAmount("EUR", 2.38), hasGrossValue("EUR", 2.91), //
                        hasForexGrossValue("USD", 3.11), //
                        hasTaxes("EUR", 0.51 + 0.02), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        Security security = new Security("VANGUARD FTSE EMU.ETF DLD", CurrencyUnit.EUR);
        security.setIsin("IE00B3VVMM84");

        Client client = new Client();
        client.addSecurity(security);

        RaisinBankAGPDFExtractor extractor = new RaisinBankAGPDFExtractor(client);

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
                        hasDate("2024-07-01T00:00"), hasShares(6.38096), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Auftragsnummer: 237dcf5e-bbe9-4127-9eef-4153d9c85ccf"), //
                        hasAmount("EUR", 2.38), hasGrossValue("EUR", 2.91), //
                        hasTaxes("EUR", 0.51 + 0.02), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }
}
