package name.abuchen.portfolio.datatransfer.pdf.santanderconsumerbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
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

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SantanderConsumerBankPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class SantanderConsumerBankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new SantanderConsumerBankPDFExtractor(new Client());

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
                        hasIsin("US88579Y1010"), hasWkn("851745"), hasTicker(null), //
                        hasName("3M CO. REGISTERED SHARES DL -,01"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-17T16:53:45"), hasShares(2.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftragsnummer 000000/00.00 | Limit billigst"), //
                        hasAmount("EUR", 325.86), hasGrossValue("EUR", 317.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 7.90))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new SantanderConsumerBankPDFExtractor(new Client());

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
                        hasIsin("DE0008404005"), hasWkn("840400"), hasTicker(null), //
                        hasName("ALLIANZ SE VINK.NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-17T16:38:35"), hasShares(2.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer 000000/00.00 | Limit billigst"), //
                        hasAmount("EUR", 430.80), hasGrossValue("EUR", 422.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 7.90 + 0.60))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new SantanderConsumerBankPDFExtractor(new Client());

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
                        hasIsin("US88579Y1010"), hasWkn("851745"), hasTicker(null), //
                        hasName("3M CO. REGISTERED SHARES DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-16T00:00"), hasExDate("2021-05-20T00:00"), //
                        hasShares(2.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abrechnungsnr. 00000000000 | Quartalsdividende"), //
                        hasAmount("EUR", 2.07), hasGrossValue("EUR", 2.44), //
                        hasForexGrossValue("USD", 2.96), //
                        hasTaxes("EUR", 0.37), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("3M CO. REGISTERED SHARES DL -,01", "EUR");
        security.setIsin("US88579Y1010");
        security.setWkn("851745");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SantanderConsumerBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

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
                        hasDate("2021-06-16T00:00"), hasExDate("2021-05-20T00:00"), //
                        hasShares(2.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abrechnungsnr. 00000000000 | Quartalsdividende"), //
                        hasAmount("EUR", 2.07), hasGrossValue("EUR", 2.44), //
                        hasTaxes("EUR", 0.37), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new SantanderConsumerBankPDFExtractor(new Client());

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
                        hasIsin("DE0008404005"), hasWkn("840400"), hasTicker(null), //
                        hasName("ALLIANZ SE VINK.NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-05-10T00:00"), hasExDate("2021-05-06T00:00"), //
                        hasShares(2.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungsnr. 0000000000"), //
                        hasAmount("EUR", 19.20), hasGrossValue("EUR", 19.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new SantanderConsumerBankPDFExtractor(new Client());

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
                        hasIsin("IE00B3RBWM25"), hasWkn("A1JX52"), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-06-30T00:00"), hasExDate("2023-06-15T00:00"), //
                        hasShares(495.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr. 00000000000"), //
                        hasAmount("EUR", 327.96), hasGrossValue("EUR", 327.96), //
                        hasForexGrossValue("USD", 360.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON", "EUR");
        security.setIsin("IE00B3RBWM25");
        security.setWkn("A1JX52");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SantanderConsumerBankPDFExtractor(client);

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
                        hasDate("2023-06-30T00:00"), hasExDate("2023-06-15T00:00"), //
                        hasShares(495.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr. 00000000000"), //
                        hasAmount("EUR", 327.96), hasGrossValue("EUR", 327.96), //
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
    public void testDividende04()
    {
        var extractor = new SantanderConsumerBankPDFExtractor(new Client());

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
                        hasIsin("IE00B3RBWM25"), hasWkn("A1JX52"), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-27T00:00"), hasExDate("2024-06-13T00:00"), //
                        hasShares(495.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 00000000000"), //
                        hasAmount("EUR", 291.95), hasGrossValue("EUR", 363.10), //
                        hasForexGrossValue("USD", 390.48), //
                        hasTaxes("EUR", 62.15 + 3.41 + 5.59), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON", "EUR");
        security.setIsin("IE00B3RBWM25");
        security.setWkn("A1JX52");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SantanderConsumerBankPDFExtractor(client);

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
                        hasDate("2024-06-27T00:00"), hasExDate("2024-06-13T00:00"), //
                        hasShares(495.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 00000000000"), //
                        hasAmount("EUR", 291.95), hasGrossValue("EUR", 363.10), //
                        hasTaxes("EUR", 62.15 + 3.41 + 5.59), hasFees("EUR", 0.00), //
                        check(tx -> {
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
        var extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

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
        assertThat(results, hasItem(interest( //
                        hasDate("2023-05-31"), hasShares(0), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Habenzinsen"), //
                        hasAmount("EUR", 4.97), hasGrossValue("EUR", 6.63), //
                        hasTaxes("EUR", 1.66), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-05-20"), hasAmount("EUR", 1), //
                        hasSource("Kontoauszug01.txt"), hasNote("auf Konto IBAN AT123456789012345678"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-19"), hasAmount("EUR", 34), //
                        hasSource("Kontoauszug01.txt"),
                        hasNote("von Konto AT123456789012345678 lautend auf Max Muster"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-15"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug01.txt"),
                        hasNote("von Konto AT123456789012345678 lautend auf Max Muster"))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new SantanderConsumerBankPDFExtractor(new Client());

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
        assertThat(results, hasItem(interest(hasDate("2022-12-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug02.txt"), hasNote("Habenzinsen"))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

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
        assertThat(results, hasItem(interest(hasDate("2023-01-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-02-28"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-03-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-04-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-05-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-06-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-07-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-08-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-09-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-10-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-11-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-12-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug03.txt"), hasNote("Habenzinsen"))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

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
        assertThat(results, hasItem(interest(hasDate("2017-01-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug04.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-03-31"), hasAmount("EUR", 5.87), //
                        hasSource("Kontoauszug04.txt"), hasNote("ÜBERWEISUNG"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-04-28"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug04.txt"), hasNote("ÜBERWEISUNG"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2017-06-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug04.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2017-07-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug04.txt"), hasNote("Habenzinsen"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2017-08-30"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug04.txt"), hasNote("Habenzinsen"))));

    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new SantanderConsumerBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-11-21"), hasAmount("EUR", 263.22), //
                        hasSource("Kontoauszug05.txt"), hasNote("Einzahlung von Konto bK553176205628587457"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-11-24"), hasAmount("EUR", 38000.0), //
                        hasSource("Kontoauszug05.txt"), hasNote("Einzahlung von Konto mI079617074233785537"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-11-24"), hasAmount("EUR", 8.5), //
                        hasSource("Kontoauszug05.txt"), hasNote("Einzahlung von Konto Lr976088571958963679"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-11-27"), hasAmount("EUR", 6000.0), //
                        hasSource("Kontoauszug05.txt"), hasNote("Einzahlung von Konto UH251100718800015609"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-11-30"), hasShares(0), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote("Habenzinsen"), //
                        hasAmount("EUR", 13.72), hasGrossValue("EUR", 18.29), //
                        hasTaxes("EUR", 4.57), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new SantanderConsumerBankPDFExtractor(new Client());

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

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-21"), hasAmount("EUR", 10.0), //
                        hasSource("Kontoauszug06.txt"), hasNote("Einzahlung von Konto AT123456789012345678"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-21"), hasAmount("EUR", 5000.0), //
                        hasSource("Kontoauszug06.txt"), hasNote("Einzahlung von Konto AT123456789012345678"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-21"), hasAmount("EUR", 3700.0), //
                        hasSource("Kontoauszug06.txt"), hasNote("Einzahlung von Konto AT123456789012345678"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-22"), hasAmount("EUR", 2518.75), //
                        hasSource("Kontoauszug06.txt"), hasNote("Einzahlung von Konto AT123456789012345678"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-26"), hasAmount("EUR", 400.0), //
                        hasSource("Kontoauszug06.txt"), hasNote("Einzahlung von Konto AT123456789012345678"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-30"), hasAmount("EUR", 20140.96), //
                        hasSource("Kontoauszug06.txt"), hasNote("Einzahlung von Konto AT123456789012345678"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2026-01-31"), hasShares(0), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote("Habenzinsen"), //
                        hasAmount("EUR", 6.18), hasGrossValue("EUR", 8.24), //
                        hasTaxes("EUR", 2.06), hasFees("EUR", 0.00))));
    }
}
