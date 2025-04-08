package name.abuchen.portfolio.datatransfer.pdf.gladbacherbankag;

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
import name.abuchen.portfolio.datatransfer.pdf.GladbacherBankAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class GladbacherBankAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        GladbacherBankAGPDFExtractor extractor = new GladbacherBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1713401024"), hasWkn("864371"), hasTicker(null), //
                        hasName("CHURCH & DWIGHT CO. INC. REGISTERED SHARES DL 1"), //
                        hasCurrencyCode(CurrencyUnit.EUR))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-18T18:14:20"), hasShares(28), //
                        hasSource("Kauf01.txt"), //
                        hasNote("R.-Nr.: W07512-0000013677/23 | Limit billigst"), //
                        hasAmount("EUR", 2395.10), hasGrossValue("EUR", 2380.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 15.00 + 0.10))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        GladbacherBankAGPDFExtractor extractor = new GladbacherBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007165631"), hasWkn("716563"), hasTicker(null), //
                        hasName("SARTORIUS AG VORZUGSAKTIEN O.ST. O.N."), //
                        hasCurrencyCode(CurrencyUnit.EUR))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-02-06T19:05:48"), hasShares(8), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("R.-Nr.: W07512-0000002019/23 | Limit bestens"), //
                        hasAmount("EUR", 3698.50), hasGrossValue("EUR", 3713.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 15.00 + 0.10))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        GladbacherBankAGPDFExtractor extractor = new GladbacherBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn("870747"), hasTicker(null), //
                        hasName("MICROSOFT CORP. REGISTERED SHARES DL-,00000625"), //
                        hasCurrencyCode(CurrencyUnit.EUR))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-01-23T11:20:08"), hasShares(6), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("R.-Nr.: W07512-0000001104/24 | Limit bestens"), //
                        hasAmount("EUR", 2092.61), hasGrossValue("EUR", 2219.83), //
                        hasTaxes("EUR", 35.23 + 72.89 + 4.00), hasFees("EUR", 15.00 + 0.10))));
    }

    @Test
    public void testDividende01()
    {
        GladbacherBankAGPDFExtractor extractor = new GladbacherBankAGPDFExtractor(new Client());

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
                        hasIsin("US0311001004"), hasWkn("908668"), hasTicker(null), //
                        hasName("AMETEK INC. REGISTERED SHARES DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-28T00:00"), hasShares(52), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abrechnungsnr.: 86148344230 | Quartalsdividende"), //
                        hasAmount("EUR", 8.71), hasGrossValue("EUR", 11.70), //
                        hasForexGrossValue("USD", 13.00), //
                        hasTaxes("EUR", 1.76 + 1.17 + 0.06), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        Security security = new Security("AMETEK INC. REGISTERED SHARES DL -,01", CurrencyUnit.EUR);
        security.setIsin("US0311001004");
        security.setWkn("908668");

        Client client = new Client();
        client.addSecurity(security);

        GladbacherBankAGPDFExtractor extractor = new GladbacherBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-28T00:00"), hasShares(52), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abrechnungsnr.: 86148344230 | Quartalsdividende"), //
                        hasAmount("EUR", 8.71), hasGrossValue("EUR", 11.70), //
                        hasTaxes("EUR", 1.76 + 1.17 + 0.06), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }
}
