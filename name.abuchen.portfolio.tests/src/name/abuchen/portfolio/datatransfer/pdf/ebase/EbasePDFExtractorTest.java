package name.abuchen.portfolio.datatransfer.pdf.ebase;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
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
import name.abuchen.portfolio.datatransfer.pdf.EbasePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

public class EbasePDFExtractorTest
{
    @Test
    public void testDividende01()
    {
        var extractor = new EbasePDFExtractor(new Client());

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
                        hasIsin("US36829G1076"), hasWkn("A0J4TC"), hasTicker(null), //
                        hasName("GAZPROM NEFT PJSC REG. SHS (SP.ADRS)/5 RL-,0016"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-26T00:00"), hasExDate("2021-06-24T00:00"), //
                        hasShares(180), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abrechnungsnr. 70418365490"), //
                        hasAmount("EUR", 84.05), hasGrossValue("EUR", 102.47), //
                        hasForexGrossValue("USD", 120.92), //
                        hasTaxes("EUR", 15.37), hasFees("EUR", 3.05))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("GAZPROM NEFT PJSC REG. SHS (SP.ADRS)/5 RL-,0016", "EUR");
        security.setIsin("US36829G1076");
        security.setWkn("A0J4TC");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EbasePDFExtractor(client);

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
                        hasDate("2021-07-26T00:00"), hasExDate("2021-06-24T00:00"), //
                        hasShares(180), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abrechnungsnr. 70418365490"), //
                        hasAmount("EUR", 84.05), hasGrossValue("EUR", 102.47), //
                        hasTaxes("EUR", 15.37), hasFees("EUR", 3.05), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testUmsatzabrechnung01()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BJZ2DC62"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE) - Russell Midcap Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010405431"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor MSCI Greece UCITS ETF Actions au Porteur Dist o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0378453376"), hasWkn(null), hasTicker(null), //
                        hasName("ComStage-Nikkei 225 UCITS ET99133507781F Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-21"), hasShares(12.729132), //
                        hasSource("Umsatzabrechnung01.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXX/XXXXXXXX"), //
                        hasAmount("EUR", 300.00), hasGrossValue("EUR", 299.40), //
                        hasForexGrossValue("USD", 299.40 * 1.105500), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.60))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-13"), hasShares(199.500000), //
                        hasSource("Umsatzabrechnung01.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXX/XXXXXXXX"), //
                        hasAmount("EUR", 211.05), hasGrossValue("EUR", 210.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.42))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2019-09-30"), hasShares(10), //
                        hasSource("Umsatzabrechnung01.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXX/XXXXXXXX"), //
                        hasAmount("EUR", 202.64), hasGrossValue("EUR", 202.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung01WithSecurityInEUR()
    {
        var security1 = new Security("Xtr.(IE) - Russell Midcap Registered Shares 1C USD o.N.", "EUR");
        security1.setIsin("IE00BJZ2DC62");

        var client = new Client();
        client.addSecurity(security1);

        var extractor = new EbasePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung01.txt"), errors);

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
                        hasIsin("FR0010405431"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor MSCI Greece UCITS ETF Actions au Porteur Dist o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0378453376"), hasWkn(null), hasTicker(null), //
                        hasName("ComStage-Nikkei 225 UCITS ET99133507781F Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-21"), hasShares(12.729132), //
                        hasSource("Umsatzabrechnung01.txt"), hasNote("Ref.-Nr.: XXXXXXXX/XXXXXXXX"), //
                        hasAmount("EUR", 300.00), hasGrossValue("EUR", 299.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.60))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-13"), hasShares(199.500000), //
                        hasSource("Umsatzabrechnung01.txt"), hasNote("Ref.-Nr.: XXXXXXXX/XXXXXXXX"), //
                        hasAmount("EUR", 211.05), hasGrossValue("EUR", 210.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.42))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2019-09-30"), hasShares(10), //
                        hasSource("Umsatzabrechnung01.txt"), hasNote("Ref.-Nr.: XXXXXXXX/XXXXXXXX"), //
                        hasAmount("EUR", 202.64), hasGrossValue("EUR", 202.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung02()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0D8Q49"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0592215403"), hasWkn(null), hasTicker(null), //
                        hasName("Xtrackers MSCI Philippines Inhaber-Anteile 1C-USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-02"), hasShares(1.705860), //
                        hasSource("Umsatzabrechnung02.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXX/XXXXXXXX"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 49.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.10))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-20"), hasShares(0.005395), //
                        hasSource("Umsatzabrechnung02.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXX/XXXXXXXX | Wiederanlage Fondsertrag"), //
                        hasAmount("EUR", 0.37), hasGrossValue("EUR", 0.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-24"), //
                        hasShares(0), //
                        hasSource("Umsatzabrechnung02.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXXXX/XXXXXXXXXX | Vorabpauschale zum Stichtag 31.12.2019"), //
                        hasAmount("EUR", 0.14), hasGrossValue("EUR", 0.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung03()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check security1
        assertThat(results, hasItem(security( //
                        hasIsin("LU0322253732"), hasWkn(null), hasTicker(null), //
                        hasName("Xtrackers MSCI Europe Mid Cap Inhaber-Anteile 1C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security2
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check security3
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0D8Q49"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-01-27"), hasShares(0.001162), //
                        hasSource("Umsatzabrechnung03.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXX/XXXXXXXX | Verkauf wegen Vorabpauschale"), //
                        hasAmount("EUR", 0.13), hasGrossValue("EUR", 0.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-10-22"), hasShares(1.705860), //
                        hasSource("Umsatzabrechnung03.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXXX/XXXXXXXX | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-10-16"), hasExDate(null), //
                        hasShares(1), //
                        hasSource("Umsatzabrechnung03.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXXX/XXXXXXXXX"), //
                        hasAmount("EUR", 0.29), hasGrossValue("EUR", 0.35), //
                        hasForexGrossValue("USD", 0.35 * 1.104600), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2019-10-16"), hasShares(1), //
                                        hasSource("Umsatzabrechnung03.txt"), //
                                        hasNote("Ref.-Nr.: XXXXXXXXX/XXXXXXXXX"), //
                                        hasAmount("EUR", 0.29), hasGrossValue("EUR", 0.23), //
                                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung03WithSecurityInEUR()
    {
        var security3 = new Security("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile", "EUR");
        security3.setIsin("DE000A0D8Q49");

        var client = new Client();
        client.addSecurity(security3);

        var extractor = new EbasePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0322253732"), hasWkn(null), hasTicker(null), //
                        hasName("Xtrackers MSCI Europe Mid Cap Inhaber-Anteile 1C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0F5UH1"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-01-27"), hasShares(0.001162), //
                        hasSource("Umsatzabrechnung03.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXX/XXXXXXXX | Verkauf wegen Vorabpauschale"), //
                        hasAmount("EUR", 0.13), hasGrossValue("EUR", 0.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-10-22"), hasShares(1.705860), //
                        hasSource("Umsatzabrechnung03.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXXX/XXXXXXXX | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-10-16"), hasExDate(null), //
                        hasShares(1), //
                        hasSource("Umsatzabrechnung03.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXXX/XXXXXXXXX"), //
                        hasAmount("EUR", 0.29), hasGrossValue("EUR", 0.35), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2019-10-16"), hasShares(1), //
                                        hasSource("Umsatzabrechnung03.txt"), //
                                        hasNote("Ref.-Nr.: XXXXXXXXX/XXXXXXXXX"), //
                                        hasAmount("EUR", 0.29), hasGrossValue("EUR", 0.23), //
                                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung04()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE All-World U.ETF Registered Shares USD Dis.oN"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        // ISIN Anteilsbestand Betrag je Anteil Betrag
        // IE00B3RBWM25 7,332986 0,297309 USD 2,18 USD
        // Kapitalertragsteuer Solidaritätszuschlag Kirchensteuer Devisenkurs
        // abzgl. Steuern
        // 0,34 EUR 0,01 EUR 0,00 EUR 1,117800 0,39 USD
        // Zahlungsbetrag 1,79 USD
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-01-02"), hasExDate(null), //
                        hasShares(7.332986), //
                        hasSource("Umsatzabrechnung04.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXX/XXXXXXXX"), //
                        hasAmount("USD", 1.79), hasGrossValue("USD", 2.18), //
                        hasTaxes("USD", 0.39), hasFees("USD", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2020-01-02"), hasShares(7.332986), //
                                        hasSource("Umsatzabrechnung04.txt"), //
                                        hasNote("Ref.-Nr.: XXXXXXXX/XXXXXXXX"), //
                                        hasAmount("USD", 1.79), hasGrossValue("USD", 1.48), //
                                        hasTaxes("USD", 0.31), hasFees("USD", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung05()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung05.txt"), errors);

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
                        hasIsin("LU0274211480"), hasWkn(null), hasTicker(null), //
                        hasName("db x-trackers DAX ETF (DR) Inhaber-Anteile 1C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-05-03"), hasShares(0.122649), //
                        hasSource("Umsatzabrechnung05.txt"), //
                        hasNote("Ref.-Nr.: 111111111/11111111 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 15.00), hasGrossValue("EUR", 14.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.03))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-12-22"), hasShares(0.100548), //
                        hasSource("Umsatzabrechnung05.txt"), //
                        hasNote("Ref.-Nr.: 111111111/11111111 | Entgelt Verkauf"), //
                        hasAmount("EUR", 12.00), hasGrossValue("EUR", 12.79), //
                        hasTaxes("EUR", 0.75 + 0.04), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2017-12-22"), //
                        hasShares(0.100548), //
                        hasSource("Umsatzabrechnung05.txt"), //
                        hasNote("Ref.-Nr.: 111111111/11111111 | Depotführungsentgelt"), //
                        hasAmount("EUR", 12.00), hasGrossValue("EUR", 12.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung06()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung06.txt"), errors);

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
                        hasIsin("FR0010315770"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor ETF MSCI WORLD FCP Actions au Port.D-EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-06-04"), hasShares(60), //
                        hasSource("Umsatzabrechnung06.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXXX/XXXXXXXX"), //
                        hasAmount("EUR", 6193.80), hasGrossValue("EUR", 6193.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung07()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B2QWDY88"), hasWkn(null), hasTicker(null), //
                        hasName("iShs III-MSCI Jap.Sm.Cap U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        // ISIN Anteilsbestand Betrag je Anteil Betrag
        // IE00B2QWDY88 37,327232 0,373500000 USD 13,94 USD
        // Kapitalertragsteuer Solidaritätszuschlag Kirchensteuer Devisenkurs
        // abzgl. Steuern
        // 2,97 EUR 0,16 EUR 0,00 EUR 1,177700 3,69 USD
        // Zahlungsbetrag 10,25 USD
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-30"), hasExDate(null), //
                        hasShares(37.327232), //
                        hasSource("Umsatzabrechnung07.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXXXX/XXXXXXXXXX"), //
                        hasAmount("USD", 10.25), hasGrossValue("USD", 13.94), //
                        hasTaxes("USD", 3.69), hasFees("USD", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-03"), hasShares(0.278245), //
                        hasSource("Umsatzabrechnung07.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXXXX/XXXXXXXXXX | Wiederanlage Fondsertrag"), //
                        hasAmount("USD", 10.25), hasGrossValue("USD", 10.25), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2020-07-30"), hasShares(37.327232), //
                                        hasSource("Umsatzabrechnung07.txt"), //
                                        hasNote("Ref.-Nr.: XXXXXXXXXX/XXXXXXXXXX"), //
                                        hasAmount("USD", 10.25), hasGrossValue("USD", 7.59), //
                                        hasTaxes("USD", 2.66), hasFees("USD", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung08()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005933972"), hasWkn(null), hasTicker(null), //
                        hasName("iShares TecDAX UCITS ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-29"), hasShares(1.551798), //
                        hasSource("Umsatzabrechnung08.txt"), //
                        hasNote("Ref.-Nr.: 400011237/29102020 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-27"), hasShares(1.409391), //
                        hasSource("Umsatzabrechnung08.txt"), //
                        hasNote("Ref.-Nr.: 400011151/27112020"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-23"), hasShares(0.414447), //
                        hasSource("Umsatzabrechnung08.txt"), //
                        hasNote("Ref.-Nr.: 450088642/11122020 | Entgelt Verkauf"), //
                        hasAmount("EUR", 11.70), hasGrossValue("EUR", 11.73), //
                        hasTaxes("EUR", 0.03), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-23"), hasShares(1.382295), //
                        hasSource("Umsatzabrechnung08.txt"), //
                        hasNote("Ref.-Nr.: 400006407/23122020 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-12-23"), //
                        hasShares(0.414447), //
                        hasSource("Umsatzabrechnung08.txt"), //
                        hasNote("Ref.-Nr.: 450088642/11122020 | Depotführungsentgelt"), //
                        hasAmount("EUR", 11.70), hasGrossValue("EUR", 11.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung09()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B1W57M07"), hasWkn(null), hasTicker(null), //
                        hasName("iShsII- BRIC 50 UCITS ETF Registered Shs USD (Dist) o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        // ISIN Anteilsbestand Betrag je Anteil Betrag
        // IE00B1W57M07 12,000000 0,661400 USD 7,94 USD
        // Kapitalertragsteuer Solidaritätszuschlag Kirchensteuer Devisenkurs
        // abzgl. Steuern
        // 1,53 EUR 0,08 EUR 0,00 EUR 1,103900 1,78 USD
        // Zahlungsbetrag 6,16 USD
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-28"), hasExDate(null), //
                        hasShares(12), //
                        hasSource("Umsatzabrechnung09.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXXXX/XXXXXXXXXX"), //
                        hasAmount("USD", 6.16), hasGrossValue("USD", 7.94), //
                        hasTaxes("USD", 1.78), hasFees("USD", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02"), hasShares(0.200653), //
                        hasSource("Umsatzabrechnung09.txt"), //
                        hasNote("Ref.-Nr.: XXXXXXXXXX/XXXXXXXXXX | Wiederanlage Fondsertrag"), //
                        hasAmount("USD", 6.16), hasGrossValue("USD", 6.16), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2019-11-28"), hasShares(12), //
                                        hasSource("Umsatzabrechnung09.txt"), //
                                        hasNote("Ref.-Nr.: XXXXXXXXXX/XXXXXXXXXX"), //
                                        hasAmount("USD", 6.16), hasGrossValue("USD", 4.70), //
                                        hasTaxes("USD", 1.46), hasFees("USD", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung10()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N."), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-19"), hasShares(0.703198), //
                        hasSource("Umsatzabrechnung10.txt"), //
                        hasNote("Ref.-Nr.: xxx/16102020 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasForexGrossValue("USD", 39.92 * 1.173700), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-17"), hasShares(0.678808), //
                        hasSource("Umsatzabrechnung10.txt"), //
                        hasNote("Ref.-Nr.: xxx/16112020"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasForexGrossValue("USD", 39.92 * 1.184800), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-17"), hasShares(0.674710), //
                        hasSource("Umsatzabrechnung10.txt"), //
                        hasNote("Ref.-Nr.: xxx/16122020"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasForexGrossValue("USD", 39.92 * 1.220800), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-23"), hasShares(0.167976), //
                        hasSource("Umsatzabrechnung10.txt"), //
                        hasNote("Ref.-Nr.: xxx/11122020 | Entgelt Verkauf"), //
                        hasAmount("EUR", 9.75), hasGrossValue("EUR", 9.75), //
                        hasForexGrossValue("USD", 9.75 * 1.227400), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-12-23"), //
                        hasShares(0.167976), //
                        hasSource("Umsatzabrechnung10.txt"), //
                        hasNote("Ref.-Nr.: xxx/11122020 | VL-Vertragsentgelt"), //
                        hasAmount("EUR", 9.75), hasGrossValue("EUR", 9.75), //
                        hasForexGrossValue("USD", 11.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung10WithSecurityInEUR()
    {
        var security = new Security("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N.", "EUR");
        security.setIsin("IE00B4L5Y983");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EbasePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-19"), hasShares(0.703198), //
                        hasSource("Umsatzabrechnung10.txt"), //
                        hasNote("Ref.-Nr.: xxx/16102020 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-17"), hasShares(0.678808), //
                        hasSource("Umsatzabrechnung10.txt"), hasNote("Ref.-Nr.: xxx/16112020"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-17"), hasShares(0.674710), //
                        hasSource("Umsatzabrechnung10.txt"), hasNote("Ref.-Nr.: xxx/16122020"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-23"), hasShares(0.167976), //
                        hasSource("Umsatzabrechnung10.txt"), hasNote("Ref.-Nr.: xxx/11122020 | Entgelt Verkauf"), //
                        hasAmount("EUR", 9.75), hasGrossValue("EUR", 9.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-12-23"), //
                        hasShares(0.167976), //
                        hasSource("Umsatzabrechnung10.txt"), //
                        hasNote("Ref.-Nr.: xxx/11122020 | VL-Vertragsentgelt"), //
                        hasAmount("EUR", 9.75), hasGrossValue("EUR", 9.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung11()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung11.txt"), errors);

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
                        hasIsin("DE0002635307"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.STOXX Europe 600 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-04-06"), hasShares(9.999999), //
                        hasSource("Umsatzabrechnung11.txt"), //
                        hasNote("Ref.-Nr.: 000000000/000000000 | Entgeltbelastung Verkauf"), //
                        hasAmount("EUR", 3.00), hasGrossValue("EUR", 3.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-04-06"), //
                        hasShares(9.999999), //
                        hasSource("Umsatzabrechnung11.txt"), //
                        hasNote("Ref.-Nr.: 000000000/000000000 | Depotführungsentgelt"), //
                        hasAmount("EUR", 3.00), hasGrossValue("EUR", 3.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung12()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung12.txt"), errors);

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
                        hasIsin("DE000A0F5UF5"), hasWkn(null), hasTicker(null), //
                        hasName("iShare.NASDAQ-100 UCITS ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-03"), hasShares(0.047085), //
                        hasSource("Umsatzabrechnung12.txt"), //
                        hasNote("Ref.-Nr.: 987654321/03012022 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 6.65), hasGrossValue("EUR", 6.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.01))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-07"), hasShares(0.022249), //
                        hasSource("Umsatzabrechnung12.txt"), //
                        hasNote("Ref.-Nr.: 987654323/06012022 | Entgeltbelastung Verkauf"), //
                        hasAmount("EUR", 3.00), hasGrossValue("EUR", 3.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2022-01-07"), //
                        hasShares(0.022249), //
                        hasSource("Umsatzabrechnung12.txt"), //
                        hasNote("Ref.-Nr.: 987654323/06012022 | Depotführungsentgelt"), //
                        hasAmount("EUR", 3.00), hasGrossValue("EUR", 3.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung13()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung13.txt"), errors);

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
                        hasIsin("DE000A0F5UF5"), hasWkn(null), hasTicker(null), //
                        hasName("iShare.NASDAQ-100 UCITS ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-07-05"), hasShares(0.024859), //
                        hasSource("Umsatzabrechnung13.txt"), //
                        hasNote("Ref.-Nr.: 123456789/123456789 | Entgeltbelastung Verkauf"), //
                        hasAmount("EUR", 3.00), hasGrossValue("EUR", 3.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-07-05"), //
                        hasShares(0.024859), //
                        hasSource("Umsatzabrechnung13.txt"), //
                        hasNote("Ref.-Nr.: 123456789/123456789 | Depotführungsentgelt"), //
                        hasAmount("EUR", 3.00), hasGrossValue("EUR", 3.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung14()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung14.txt"), errors);

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
                        hasIsin("DE000A0F5UF5"), hasWkn(null), hasTicker(null), //
                        hasName("iShare.NASDAQ-100 UCITS ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-02"), hasShares(1234.1234), //
                        hasSource("Umsatzabrechnung14.txt"), //
                        hasNote("Ref.-Nr.: 123456789/123456789 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 1234.12), hasGrossValue("EUR", 1234.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));
    }

    @Test
    public void testUmsatzabrechnung15()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(8L));
        assertThat(countBuySell(results), is(18L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(28));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0159052710"), hasWkn(null), hasTicker(null), //
                        hasName("JPMorgan-US Technology Fund Actions Nom. A (acc.) EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0552385295"), hasWkn(null), hasTicker(null), //
                        hasName("Mor.St.Inv.-Global Opportunity Actions Nominatives A USD o.N."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0171310443"), hasWkn(null), hasTicker(null), //
                        hasName("BGF - World Technology Fund Act. Nom. Classe A2 EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0073232471"), hasWkn(null), hasTicker(null), //
                        hasName("Mor.St.Inv.-US Growth Fund Actions Nom. A o.N."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009769869"), hasWkn(null), hasTicker(null), //
                        hasName("DWS Akt.Strategie Deutschl. Inhaber-Anteile LC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010527275"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor World Water(DR)UCITS ETF Actions au Port.Dist o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009772657"), hasWkn(null), hasTicker(null), //
                        hasName("Siemens Global Growth Inhaber-Anteile A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009807016"), hasWkn(null), hasTicker(null), //
                        hasName("hausInvest Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-06"), hasShares(0.138927), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31032021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-06"), hasShares(0.879612), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31032021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasForexGrossValue("USD", 100.00 * 1.177800), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-06"), hasShares(1.510346), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31032021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-06"), hasShares(0.221967), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31032021"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasForexGrossValue("USD", 50.00 * 1.177800), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(0.048774), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/30042021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(0.483285), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/30042021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-04"), hasShares(2.869440), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/03052021 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-04"), hasShares(0.142698), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/30042021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-04"), hasShares(0.865179), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/30042021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasForexGrossValue("USD", 119.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-04"), hasShares(1.529988), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/30042021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-04"), hasShares(0.223201), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/30042021"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasForexGrossValue("USD", 59.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(0.047562), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31052021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(0.475691), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31052021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(1.576541), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31052021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-02"), hasShares(2.939015), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31052021 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-02"), hasShares(0.146836), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/28052021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-02"), hasShares(0.881525), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31052021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasForexGrossValue("USD", 121.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 18th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-02"), hasShares(0.229964), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31052021"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasForexGrossValue("USD", 60.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-14"), hasExDate(null), //
                        hasShares(0.049184), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/14062021"), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2021-06-14"), hasShares(0.049184), //
                                        hasSource("Umsatzabrechnung15.txt"), //
                                        hasNote("Ref.-Nr.: 000/14062021 | vermögenswirksame Leistungen"), //
                                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung15WithSecurityInEUR()
    {
        var security2 = new Security("Mor.St.Inv.-Global Opportunity Actions Nominatives A USD o.N.", "EUR");
        security2.setIsin("LU0552385295");

        var security4 = new Security("Mor.St.Inv.-US Growth Fund Actions Nom. A o.N.", "EUR");
        security4.setIsin("LU0073232471");

        var client = new Client();
        client.addSecurity(security2);
        client.addSecurity(security4);

        var extractor = new EbasePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(6L));
        assertThat(countBuySell(results), is(18L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(26));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0159052710"), hasWkn(null), hasTicker(null), //
                        hasName("JPMorgan-US Technology Fund Actions Nom. A (acc.) EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0171310443"), hasWkn(null), hasTicker(null), //
                        hasName("BGF - World Technology Fund Act. Nom. Classe A2 EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009769869"), hasWkn(null), hasTicker(null), //
                        hasName("DWS Akt.Strategie Deutschl. Inhaber-Anteile LC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010527275"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor World Water(DR)UCITS ETF Actions au Port.Dist o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009772657"), hasWkn(null), hasTicker(null), //
                        hasName("Siemens Global Growth Inhaber-Anteile A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009807016"), hasWkn(null), hasTicker(null), //
                        hasName("hausInvest Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-06"), hasShares(0.138927), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31032021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-06"), hasShares(0.879612), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31032021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-06"), hasShares(1.510346), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31032021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-06"), hasShares(0.221967), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31032021"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(0.048774), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/30042021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(0.483285), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/30042021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-04"), hasShares(2.869440), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/03052021 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-04"), hasShares(0.142698), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/30042021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-04"), hasShares(0.865179), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/30042021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-04"), hasShares(1.529988), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/30042021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-04"), hasShares(0.223201), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/30042021"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(0.047562), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31052021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(0.475691), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31052021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(1.576541), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31052021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-02"), hasShares(2.939015), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31052021 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-02"), hasShares(0.146836), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/28052021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-02"), hasShares(0.881525), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31052021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 18th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-02"), hasShares(0.229964), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/31052021"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-14"), hasExDate(null), //
                        hasShares(0.049184), //
                        hasSource("Umsatzabrechnung15.txt"), //
                        hasNote("Ref.-Nr.: 000/14062021"), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2021-06-14"), hasShares(0.049184), //
                                        hasSource("Umsatzabrechnung15.txt"), //
                                        hasNote("Ref.-Nr.: 000/14062021 | vermögenswirksame Leistungen"), //
                                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung16()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(8L));
        assertThat(countBuySell(results), is(22L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(35));
        /**
         * security4 The advance tax payment is always paid in local currency.
         * If the security is in foreign currency, the exchange rate is missing
         * in the document for posting.
         */
        // new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009769869"), hasWkn(null), hasTicker(null), //
                        hasName("DWS Akt.Strategie Deutschl. Inhaber-Anteile LC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009772657"), hasWkn(null), hasTicker(null), //
                        hasName("Siemens Global Growth Inhaber-Anteile A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0159052710"), hasWkn(null), hasTicker(null), //
                        hasName("JPMorgan-US Technology Fund Actions Nom. A (acc.) EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0552385295"), hasWkn(null), hasTicker(null), //
                        hasName("Mor.St.Inv.-Global Opportunity Actions Nominatives A USD o.N."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0171310443"), hasWkn(null), hasTicker(null), //
                        hasName("BGF - World Technology Fund Act. Nom. Classe A2 EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010527275"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor World Water(DR)UCITS ETF Actions au Port.Dist o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-25"), hasShares(0.002155), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/22012021 | Verkauf wegen Vorabpauschale"), //
                        hasAmount("EUR", 0.94), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-26"), hasShares(0.002987), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/22012021 | Verkauf wegen Vorabpauschale"), //
                        hasAmount("EUR", 0.04), hasGrossValue("EUR", 0.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-26"), hasShares(0.000110), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/22012021 | Verkauf wegen Vorabpauschale"), //
                        hasAmount("EUR", 0.07), hasGrossValue("EUR", 0.08), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-26"), hasShares(0.000268), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/22012021 | Verkauf wegen Vorabpauschale"), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                        hasForexGrossValue("USD", 0.03 * 1.216800), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-25"), hasShares(0.000143), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/22012021 | Verkauf wegen Vorabpauschale"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-01"), hasShares(0.525761), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450159982/29012021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-01"), hasShares(3.084040), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/29012021 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-02"), hasShares(0.054216), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450083460/29012021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-02"), hasShares(0.142943), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450246382/29012021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-02"), hasShares(1.494992), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450089736/29012021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-02"), hasShares(0.224522), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450061638/29012021"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasForexGrossValue("USD", 59.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-02"), hasShares(1.175007), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450263877/29012021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasForexGrossValue("USD", 119.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-01"), hasShares(0.053239), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450083904/26022021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-01"), hasShares(0.520901), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450161611/26022021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-01"), hasShares(3.037206), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/26022021 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-01"), hasShares(1.473622), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450089969/26022021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-02"), hasShares(0.137031), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450249782/26022021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 18th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-02"), hasShares(0.211233), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450061809/26022021"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasForexGrossValue("USD", 60.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 19th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-03-02"), hasShares(1.117828), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450267643/26022021"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasForexGrossValue("USD", 120.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 20th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(0.049875), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450085597/31032021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 21th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(0.488794), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 450165636/31032021"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));

        // check 22th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(2.947679), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/31032021 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-01-22"), //
                        hasShares(0), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/22012021 | Vorabpauschale zum Stichtag 31.12.2020"), //
                        hasAmount("EUR", 0.94), hasGrossValue("EUR", 0.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-01-22"), //
                        hasShares(0), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/22012021 | Vorabpauschale zum Stichtag 31.12.2020"), //
                        hasAmount("EUR", 0.04), hasGrossValue("EUR", 0.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-01-22"), //
                        hasShares(0), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/22012021 | Vorabpauschale zum Stichtag 31.12.2020"), //
                        hasAmount("EUR", 0.07), hasGrossValue("EUR", 0.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-01-22"), //
                        hasShares(0), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/22012021 | Vorabpauschale zum Stichtag 31.12.2020"), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-01-22"), //
                        hasShares(0), //
                        hasSource("Umsatzabrechnung16.txt"), //
                        hasNote("Ref.-Nr.: 000/22012021 | Vorabpauschale zum Stichtag 31.12.2020"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0073232471"), hasWkn(null), hasTicker(null), //
                        hasName("Mor.St.Inv.-US Growth Fund Actions Nom. A o.N."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0868753731"), hasWkn(null), hasTicker(null), //
                        hasName("MS Invt Fds-Global Advantage Actions Nom. A USD o.N."), //
                        hasCurrencyCode("USD"))));
    }

    @Test
    public void testUmsatzabrechnung17()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(11L));
        assertThat(countBuySell(results), is(11L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(24));
        new AssertImportActions().check(results, "EUR", "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008486655"), hasWkn(null), hasTicker(null), //
                        hasName("DWS Concept GS&P Food Inhaber-Anteile LD"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0069450822"), hasWkn(null), hasTicker(null), //
                        hasName("Fidelity Fds-America Fund Reg.Shs A-Euro-(Glob.Cert.)oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0571085413"), hasWkn(null), hasTicker(null), //
                        hasName("Vontobel Fd.-mtx Sust.EM Lead. Actions Nom. B Cap.USD o.N."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009769869"), hasWkn(null), hasTicker(null), //
                        hasName("DWS Akt.Strategie Deutschl. Inhaber-Anteile LC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010527275"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor World Water(DR)UCITS ETF Actions au Port.Dist o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009772657"), hasWkn(null), hasTicker(null), //
                        hasName("Siemens Global Growth Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B0MY6T00"), hasWkn(null), hasTicker(null), //
                        hasName("BNY Mellon Global Income Fund Registered Sterling Inc.Shs oN"), //
                        hasCurrencyCode("GBP"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0171310443"), hasWkn(null), hasTicker(null), //
                        hasName("BGF - World Technology Fund Act. Nom. Classe A2 EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0159052710"), hasWkn(null), hasTicker(null), //
                        hasName("JPMorgan-US Technology Fund Actions Nom. A (acc.) EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0552385295"), hasWkn(null), hasTicker(null), //
                        hasName("Mor.St.Inv.-Global Opportunity Actions Nominatives A USD o.N."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2197908721"), hasWkn(null), hasTicker(null), //
                        hasName("M.U.L.-Lyxor Nasdaq-100 UC.ETF Act. Nom. USD Acc. oN"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-27"), hasShares(0.793716), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/26112020"), //
                        hasAmount("EUR", 270.80), hasGrossValue("EUR", 272.72), //
                        hasTaxes("EUR", 1.82 + 0.10), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-27"), hasShares(9.887676), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/26112020"), //
                        hasAmount("EUR", 93.60), hasGrossValue("EUR", 93.63), //
                        hasTaxes("EUR", 0.03), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-27"), hasShares(4.954360), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/25112020"), //
                        hasAmount("EUR", 733.87), hasGrossValue("EUR", 754.74), //
                        hasForexGrossValue("USD", 754.74 * 1.195300), //
                        hasTaxes("EUR", 19.78 + 1.09), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-01"), hasShares(0.033610), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020"), //
                        hasAmount("EUR", 15.00), hasGrossValue("EUR", 15.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-01"), hasShares(0.325715), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020"), //
                        hasAmount("EUR", 15.00), hasGrossValue("EUR", 14.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.03))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-01"), hasShares(3.202562), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-03"), hasShares(1.384890), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/01122020 | Wiederanlage Fondsertrag"), //
                        hasAmount("GBP", 3.03), hasGrossValue("GBP", 3.03), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-01"), hasShares(0.414731), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-02"), hasShares(0.038637), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-02"), hasShares(0.373877), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasForexGrossValue("USD", 40.00 * 1.202200), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-02"), hasShares(0.137606), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 15.00), hasGrossValue("EUR", 14.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.03))));

        // check 1st taxes transaction
        // ISIN Anteilsbestand Betrag je Anteil Betrag
        // GB00B0MY6T00 300,991871 0,012407000 GBP 3,73 GBP
        // Kapitalertragsteuer Solidaritätszuschlag Kirchensteuer Devisenkurs
        // abzgl. Steuern
        // 0,73 EUR 0,04 EUR 0,00 EUR 0,899700 0,70 GBP
        // Zahlungsbetrag 3,03 GBP
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-01"), hasExDate(null), //
                        hasShares(300.991871), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/01122020"), //
                        hasAmount("GBP", 3.03), hasGrossValue("GBP", 3.73), //
                        hasTaxes("GBP", 0.70), hasFees("GBP", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2020-12-01"), hasShares(300.991871), //
                                        hasSource("Umsatzabrechnung17.txt"), //
                                        hasNote("Ref.-Nr.: 000/01122020"), //
                                        hasAmount("GBP", 3.03), hasGrossValue("GBP", 2.18), //
                                        hasTaxes("GBP", 0.85), hasFees("GBP", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung17WithSecurityInEUR()
    {
        var security3 = new Security("Vontobel Fd.-mtx Sust.EM Lead. Actions Nom. B Cap.USD o.N.", "EUR");
        security3.setIsin("LU0571085413");

        var security10 = new Security("Mor.St.Inv.-Global Opportunity Actions Nominatives A USD o.N.", "EUR");
        security10.setIsin("LU0552385295");

        var client = new Client();
        client.addSecurity(security3);
        client.addSecurity(security10);

        var extractor = new EbasePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(9L));
        assertThat(countBuySell(results), is(11L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(22));
        new AssertImportActions().check(results, "EUR", "GBP");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008486655"), hasWkn(null), hasTicker(null), //
                        hasName("DWS Concept GS&P Food Inhaber-Anteile LD"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0069450822"), hasWkn(null), hasTicker(null), //
                        hasName("Fidelity Fds-America Fund Reg.Shs A-Euro-(Glob.Cert.)oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009769869"), hasWkn(null), hasTicker(null), //
                        hasName("DWS Akt.Strategie Deutschl. Inhaber-Anteile LC"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010527275"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor World Water(DR)UCITS ETF Actions au Port.Dist o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009772657"), hasWkn(null), hasTicker(null), //
                        hasName("Siemens Global Growth Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B0MY6T00"), hasWkn(null), hasTicker(null), //
                        hasName("BNY Mellon Global Income Fund Registered Sterling Inc.Shs oN"), //
                        hasCurrencyCode("GBP"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0171310443"), hasWkn(null), hasTicker(null), //
                        hasName("BGF - World Technology Fund Act. Nom. Classe A2 EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0159052710"), hasWkn(null), hasTicker(null), //
                        hasName("JPMorgan-US Technology Fund Actions Nom. A (acc.) EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2197908721"), hasWkn(null), hasTicker(null), //
                        hasName("M.U.L.-Lyxor Nasdaq-100 UC.ETF Act. Nom. USD Acc. oN"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-27"), hasShares(0.793716), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/26112020"), //
                        hasAmount("EUR", 270.80), hasGrossValue("EUR", 272.72), //
                        hasTaxes("EUR", 1.82 + 0.10), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-27"), hasShares(9.887676), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/26112020"), //
                        hasAmount("EUR", 93.60), hasGrossValue("EUR", 93.63), //
                        hasTaxes("EUR", 0.03), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-27"), hasShares(4.954360), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/25112020"), //
                        hasAmount("EUR", 733.87), hasGrossValue("EUR", 754.74), //
                        hasTaxes("EUR", 19.78 + 1.09), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-01"), hasShares(0.033610), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020"), //
                        hasAmount("EUR", 15.00), hasGrossValue("EUR", 15.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-01"), hasShares(0.325715), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020"), //
                        hasAmount("EUR", 15.00), hasGrossValue("EUR", 14.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.03))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-01"), hasShares(3.202562), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-03"), hasShares(1.384890), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/01122020 | Wiederanlage Fondsertrag"), //
                        hasAmount("GBP", 3.03), hasGrossValue("GBP", 3.03), //
                        hasTaxes("GBP", 0.00), hasFees("GBP", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-01"), hasShares(0.414731), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-02"), hasShares(0.038637), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-02"), hasShares(0.373877), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-02"), hasShares(0.137606), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/30112020 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 15.00), hasGrossValue("EUR", 14.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.03))));

        // check 1st taxes transaction
        // ISIN Anteilsbestand Betrag je Anteil Betrag
        // GB00B0MY6T00 300,991871 0,012407000 GBP 3,73 GBP
        // Kapitalertragsteuer Solidaritätszuschlag Kirchensteuer Devisenkurs
        // abzgl. Steuern
        // 0,73 EUR 0,04 EUR 0,00 EUR 0,899700 0,70 GBP
        // Zahlungsbetrag 3,03 GBP
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-01"), hasExDate(null), //
                        hasShares(300.991871), //
                        hasSource("Umsatzabrechnung17.txt"), //
                        hasNote("Ref.-Nr.: 000/01122020"), //
                        hasAmount("GBP", 3.03), hasGrossValue("GBP", 3.73), //
                        hasTaxes("GBP", 0.70), hasFees("GBP", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2020-12-01"), hasShares(300.991871), //
                                        hasSource("Umsatzabrechnung17.txt"), //
                                        hasNote("Ref.-Nr.: 000/01122020"), //
                                        hasAmount("GBP", 3.03), hasGrossValue("GBP", 2.18), //
                                        hasTaxes("GBP", 0.85), hasFees("GBP", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung18()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung18.txt"), errors);

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

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-15"), hasShares(20.422296), //
                        hasSource("Umsatzabrechnung18.txt"), //
                        hasNote("Ref.-Nr.: 400065846/13102021"), //
                        hasAmount("EUR", 5000.00), hasGrossValue("EUR", 5178.07), //
                        hasTaxes("EUR", 168.79 + 9.28), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung19()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung19.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009785303"), hasWkn(null), hasTicker(null), //
                        hasName("NORDINTERNET Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0171310443"), hasWkn(null), hasTicker(null), //
                        hasName("BGF - World Technology Fund Act. Nom. Classe A2 EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-28"), hasShares(7.101511), //
                        hasSource("Umsatzabrechnung19.txt"), //
                        hasNote("Ref.-Nr.: 000/21122020"), //
                        hasAmount("EUR", 1191.18), hasGrossValue("EUR", 1272.87), //
                        hasTaxes("EUR", 77.44 + 4.25), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-28"), hasShares(18.568667), //
                        hasSource("Umsatzabrechnung19.txt"), //
                        hasNote("Ref.-Nr.: 000/21122020"), //
                        hasAmount("EUR", 1191.18), hasGrossValue("EUR", 1191.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung20()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung20.txt"), errors);

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
                        hasIsin("DE0005933972"), hasWkn(null), hasTicker(null), //
                        hasName("iShares TecDAX UCITS ETF DE Inhaber-Anteile (Acc.)"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-07-05"), hasShares(0.095371), //
                        hasSource("Umsatzabrechnung20.txt"), //
                        hasNote("Ref.-Nr.: 400357714/02072021 | Entgeltbelastung Verkauf"), //
                        hasAmount("EUR", 3.00), hasGrossValue("EUR", 3.07), //
                        hasTaxes("EUR", 0.07), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-07-05"), //
                        hasShares(0.095371), //
                        hasSource("Umsatzabrechnung20.txt"), //
                        hasNote("Ref.-Nr.: 400357714/02072021 | Depotführungsentgelt"), //
                        hasAmount("EUR", 3.00), hasGrossValue("EUR", 3.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung21()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung21.txt"), errors);

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
                        hasIsin("DE000A0F5UH1"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-18"), hasShares(0.082378), //
                        hasSource("Umsatzabrechnung21.txt"), //
                        hasNote("Wiederanlage Ertragsausschüttung"), //
                        hasAmount("EUR", 1.94), hasGrossValue("EUR", 1.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung22()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung22.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(8L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(13));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0392496427"), hasWkn(null), hasTicker(null), //
                        hasName("ComStage-DJ Sw.Tit.30TM U.ETF Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("CHF"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B0M62Q58"), hasWkn(null), hasTicker(null), //
                        hasName("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)o"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005933923"), hasWkn(null), hasTicker(null), //
                        hasName("iShares MDAX UCITS ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0031442068"), hasWkn(null), hasTicker(null), //
                        hasName("iShs S&P 500 UCITS ETF Registered Shares USD (Dist)o"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0603942888"), hasWkn(null), hasTicker(null), //
                        hasName("ComStage-SDAX UCITS ETF Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-30"), hasShares(1.502194), //
                        hasSource("Umsatzabrechnung22.txt"), //
                        hasNote("Ref.-Nr.: 400006077/25102018"), //
                        hasAmount("EUR", 140.07), hasGrossValue("EUR", 141.19), //
                        hasForexGrossValue("CHF", 141.19 * 1.140900), //
                        hasTaxes("EUR", 0.63 + 0.03 + 0.05), hasFees("EUR", 0.13 + 0.28))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-10-30"), hasShares(3.760425), //
                        hasSource("Umsatzabrechnung22.txt"), //
                        hasNote("Ref.-Nr.: 400006077/25102018"), //
                        hasAmount("EUR", 140.07), hasGrossValue("EUR", 139.60), //
                        hasForexGrossValue("USD", 139.60 * 1.136400), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.20 + 0.27))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-30"), hasShares(0.428056), //
                        hasSource("Umsatzabrechnung22.txt"), //
                        hasNote("Ref.-Nr.: 400006079/25102018"), //
                        hasAmount("EUR", 85.61), hasGrossValue("EUR", 85.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05 + 0.17))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-10-30"), hasShares(2.298277), //
                        hasSource("Umsatzabrechnung22.txt"), //
                        hasNote("Ref.-Nr.: 400006079/25102018"), //
                        hasAmount("EUR", 85.61), hasGrossValue("EUR", 85.32), //
                        hasForexGrossValue("USD", 85.32 * 1.136400), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.12 + 0.17))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-30"), hasShares(5.883736), //
                        hasSource("Umsatzabrechnung22.txt"), //
                        hasNote("Ref.-Nr.: 400006076/25102018"), //
                        hasAmount("EUR", 135.44), hasGrossValue("EUR", 136.40), //
                        hasForexGrossValue("USD", 155.82), //
                        hasTaxes("EUR", 0.51 + 0.03 + 0.04), hasFees("EUR", 0.11 + 0.27))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-10-30"), hasShares(3.635976), //
                        hasSource("Umsatzabrechnung22.txt"), //
                        hasNote("Ref.-Nr.: 400006076/25102018"), //
                        hasAmount("EUR", 135.44), hasGrossValue("EUR", 134.98), //
                        hasForexGrossValue("USD", 153.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.19 + 0.27))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-30"), hasShares(0.579236), //
                        hasSource("Umsatzabrechnung22.txt"), //
                        hasNote("Ref.-Nr.: 400006080/25102018"), //
                        hasAmount("EUR", 55.58), hasGrossValue("EUR", 55.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.04 + 0.11))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-10-30"), hasShares(1.492048), //
                        hasSource("Umsatzabrechnung22.txt"), //
                        hasNote("Ref.-Nr.: 400006080/25102018"), //
                        hasAmount("EUR", 55.58), hasGrossValue("EUR", 55.39), //
                        hasForexGrossValue("USD", 62.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08 + 0.11))));
    }

    @Test
    public void testUmsatzabrechnung22WithSecurityInEUR()
    {
        var security1 = new Security("ComStage-DJ Sw.Tit.30TM U.ETF Inhaber-Anteile I o.N.", "EUR");
        security1.setIsin("LU0392496427");

        var security2 = new Security("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)o", "EUR");
        security2.setIsin("IE00B0M62Q58");

        var security4 = new Security("iShs S&P 500 UCITS ETF Registered Shares USD (Dist)o", "EUR");
        security4.setIsin("IE0031442068");

        var client = new Client();
        client.addSecurity(security1);
        client.addSecurity(security2);
        client.addSecurity(security4);

        var extractor = new EbasePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung22.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(8L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005933923"), hasWkn(null), hasTicker(null), //
                        hasName("iShares MDAX UCITS ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0603942888"), hasWkn(null), hasTicker(null), //
                        hasName("ComStage-SDAX UCITS ETF Inhaber-Anteile I o.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-30"), hasShares(1.502194), //
                        hasSource("Umsatzabrechnung22.txt"), hasNote("Ref.-Nr.: 400006077/25102018"), //
                        hasAmount("EUR", 140.07), hasGrossValue("EUR", 141.19), //
                        hasTaxes("EUR", 0.63 + 0.03 + 0.05), hasFees("EUR", 0.13 + 0.28))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-10-30"), hasShares(3.760425), //
                        hasSource("Umsatzabrechnung22.txt"), hasNote("Ref.-Nr.: 400006077/25102018"), //
                        hasAmount("EUR", 140.07), hasGrossValue("EUR", 139.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.20 + 0.27))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-30"), hasShares(0.428056), //
                        hasSource("Umsatzabrechnung22.txt"), hasNote("Ref.-Nr.: 400006079/25102018"), //
                        hasAmount("EUR", 85.61), hasGrossValue("EUR", 85.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05 + 0.17))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-10-30"), hasShares(2.298277), //
                        hasSource("Umsatzabrechnung22.txt"), hasNote("Ref.-Nr.: 400006079/25102018"), //
                        hasAmount("EUR", 85.61), hasGrossValue("EUR", 85.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.12 + 0.17))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-30"), hasShares(5.883736), //
                        hasSource("Umsatzabrechnung22.txt"), hasNote("Ref.-Nr.: 400006076/25102018"), //
                        hasAmount("EUR", 135.44), hasGrossValue("EUR", 136.40), //
                        hasTaxes("EUR", 0.51 + 0.03 + 0.04), hasFees("EUR", 0.11 + 0.27))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-10-30"), hasShares(3.635976), //
                        hasSource("Umsatzabrechnung22.txt"), hasNote("Ref.-Nr.: 400006076/25102018"), //
                        hasAmount("EUR", 135.44), hasGrossValue("EUR", 134.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.19 + 0.27))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-30"), hasShares(0.579236), //
                        hasSource("Umsatzabrechnung22.txt"), hasNote("Ref.-Nr.: 400006080/25102018"), //
                        hasAmount("EUR", 55.58), hasGrossValue("EUR", 55.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.04 + 0.11))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-10-30"), hasShares(1.492048), //
                        hasSource("Umsatzabrechnung22.txt"), hasNote("Ref.-Nr.: 400006080/25102018"), //
                        hasAmount("EUR", 55.58), hasGrossValue("EUR", 55.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08 + 0.11))));
    }

    @Test
    public void testUmsatzabrechnung23()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE All-World U.ETF Reg. Shs USD Acc. oN"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-03"), hasShares(2.276295), //
                        hasSource("Umsatzabrechnung23.txt"), //
                        hasNote("Ref.-Nr.: XXX/XXX"), //
                        hasAmount("EUR", 219.00), hasGrossValue("EUR", 218.56), //
                        hasForexGrossValue("USD", 218.56 * 1.185800), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.44))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-09"), hasShares(1.025198), //
                        hasSource("Umsatzabrechnung23.txt"), //
                        hasNote("Ref.-Nr.: XXX/XXX"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 99.80), //
                        hasForexGrossValue("USD", 99.80 * 1.172300), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.20))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-09-02"), hasShares(2.222628), //
                        hasSource("Umsatzabrechnung23.txt"), //
                        hasNote("Ref.-Nr.: XXXX/XXXX"), //
                        hasAmount("EUR", 219.00), hasGrossValue("EUR", 218.56), //
                        hasForexGrossValue("USD", 218.56 * 1.182500), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.44))));
    }

    @Test
    public void testUmsatzabrechnung23WithSecurityInEUR()
    {
        var security = new Security("Vanguard FTSE All-World U.ETF Reg. Shs USD Acc. oN", "EUR");
        security.setIsin("IE00BK5BQT80");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EbasePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-03"), hasShares(2.276295), //
                        hasSource("Umsatzabrechnung23.txt"), hasNote("Ref.-Nr.: XXX/XXX"), //
                        hasAmount("EUR", 219.00), hasGrossValue("EUR", 218.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.44))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-09"), hasShares(1.025198), //
                        hasSource("Umsatzabrechnung23.txt"), hasNote("Ref.-Nr.: XXX/XXX"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 99.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.20))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-09-02"), hasShares(2.222628), //
                        hasSource("Umsatzabrechnung23.txt"), hasNote("Ref.-Nr.: XXXX/XXXX"), //
                        hasAmount("EUR", 219.00), hasGrossValue("EUR", 218.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.44))));
    }

    @Test
    public void testUmsatzabrechnung24()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung24.txt"), errors);

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
                        hasIsin("DE000A0F5UF5"), hasWkn(null), hasTicker(null), //
                        hasName("iShare.NASDAQ-100 UCITS ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-17"), hasExDate(null), //
                        hasShares(27.707360), //
                        hasSource("Umsatzabrechnung24.txt"), //
                        hasNote("Ref.-Nr.: xxxx/xxxx"), //
                        hasAmount("EUR", 4.37), hasGrossValue("EUR", 5.35), //
                        hasForexGrossValue("USD", 5.62), //
                        hasTaxes("EUR", 0.93 + 0.05), hasFees("EUR", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2022-06-17"), hasShares(27.70736), //
                                        hasSource("Umsatzabrechnung24.txt"), //
                                        hasNote("Ref.-Nr.: xxxx/xxxx"), //
                                        hasAmount("EUR", 4.37), hasGrossValue("EUR", 3.39), //
                                        hasTaxes("EUR", 0.98), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung24WithSecurityInEUR()
    {
        var security = new Security("iShare.NASDAQ-100 UCITS ETF DE Inhaber-Anteil", "EUR");
        security.setIsin("DE000A0F5UF5");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EbasePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-17"), hasExDate(null), //
                        hasShares(27.707360), //
                        hasSource("Umsatzabrechnung24.txt"), //
                        hasNote("Ref.-Nr.: xxxx/xxxx"), //
                        hasAmount("EUR", 4.37), hasGrossValue("EUR", 5.35), //
                        hasTaxes("EUR", 0.93 + 0.05), hasFees("EUR", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2022-06-17"), hasShares(27.70736), //
                                        hasSource("Umsatzabrechnung24.txt"), //
                                        hasNote("Ref.-Nr.: xxxx/xxxx"), //
                                        hasAmount("EUR", 4.37), hasGrossValue("EUR", 3.39), //
                                        hasTaxes("EUR", 0.98), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung25()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung25.txt"), errors);

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
                        hasIsin("DE000A0F5UF5"), hasWkn(null), hasTicker(null), //
                        hasName("iShare.NASDAQ-100 UCITS ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-20"), hasShares(0.041696), //
                        hasSource("Umsatzabrechnung25.txt"), //
                        hasNote("Ref.-Nr.: xxxx/xxxx | Wiederanlage Fondsertrag"), //
                        hasAmount("EUR", 4.37), hasGrossValue("EUR", 4.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung26()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung26.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0533033667"), hasWkn(null), hasTicker(null), //
                        hasName("MUL-LYX.MSCI Wd.IN.T.TR UC.ETF Nam.-An. ACC EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU1834983550"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor IF-L.ST.Eu.600 Bas.Res. Act. Nom. EUR Acc. oN"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-31T00:00"), hasShares(8.584242), //
                        hasSource("Umsatzabrechnung26.txt"), //
                        hasNote("Ref.-Nr.: 0400238042/31082023"), //
                        hasAmount("EUR", 5000.00), hasGrossValue("EUR", 4990.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.98))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-31T00:00"), hasShares(48.088457), //
                        hasSource("Umsatzabrechnung26.txt"), //
                        hasNote("Ref.-Nr.: 0400039899/30082023"), //
                        hasAmount("EUR", 4000.00), hasGrossValue("EUR", 3992.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 7.98))));
    }

    @Test
    public void testUmsatzabrechnung27()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung27.txt"), errors);

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
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE All-World U.ETF Registered Shares USD Dis.oN"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-23T00:00"), hasShares(30.000000), //
                        hasSource("Umsatzabrechnung27.txt"), //
                        hasNote("Ref.-Nr.: 0400036212/21082023"), //
                        hasAmount("EUR", 3078.84), hasGrossValue("EUR", 3072.69), //
                        hasForexGrossValue("USD", 3306.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.15))));
    }

    @Test
    public void testUmsatzabrechnung27WithSecurityInEUR()
    {
        var security = new Security("Vanguard FTSE All-World U.ETF Registered Shares USD Dis.oN", "EUR");
        security.setIsin("IE00B3RBWM25");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EbasePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung27.txt"), errors);

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
                        hasDate("2023-08-23T00:00"), hasShares(30.000000), //
                        hasSource("Umsatzabrechnung27.txt"), //
                        hasNote("Ref.-Nr.: 0400036212/21082023"), //
                        hasAmount("EUR", 3078.84), hasGrossValue("EUR", 3072.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.15), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testUmsatzabrechnung28()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0533033667"), hasWkn(null), hasTicker(null), //
                        hasName("MUL-LYX.MSCI Wd.IN.T.TR UC.ETF Nam.-An. ACC EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU2572257124"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi.I.S. MSCI World3 Act.Nom. U.ETF USD Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B5BMR087"), hasWkn(null), hasTicker(null), //
                        hasName("iShs VII-Core S&P 500 U.ETF Reg. Shares USD (Acc) o.N."), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE All-World U.ETF Registered Shares USD Dis.oN"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-17T00:00"), hasShares(0.2631950), //
                        hasSource("Umsatzabrechnung28.txt"), //
                        hasNote("Ref.-Nr.: 0400084351/14072023"), //
                        hasAmount("EUR", 150.00), hasGrossValue("EUR", 149.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.30))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-18T00:00"), hasShares(1.302784), //
                        hasSource("Umsatzabrechnung28.txt"), //
                        hasNote("Ref.-Nr.: 0400124334/14072023"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 99.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.20))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-01T00:00"), hasShares(4.563874), //
                        hasSource("Umsatzabrechnung28.txt"), //
                        hasNote("Ref.-Nr.: 0400000608/31072023"), //
                        hasAmount("EUR", 2000.00), hasGrossValue("EUR", 1996.01), //
                        hasForexGrossValue("USD", 2182.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.99))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-01T00:00"), hasShares(18.81825900), //
                        hasSource("Umsatzabrechnung28.txt"), //
                        hasNote("Ref.-Nr.: 0400001107/31072023"), //
                        hasAmount("EUR", 2000.00), hasGrossValue("EUR", 1996.01), //
                        hasForexGrossValue("USD", 2182.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.99))));
    }

    @Test
    public void testUmsatzabrechnung28WithSecurityInEUR()
    {
        var security3 = new Security("iShs VII-Core S&P 500 U.ETF Reg. Shares USD (Acc) o.N.", "EUR");
        security3.setIsin("IE00B5BMR087");

        var security4 = new Security("Vanguard FTSE All-World U.ETF Registered Shares USD Dis.oN", "EUR");
        security4.setIsin("IE00B3RBWM25");

        var client = new Client();
        client.addSecurity(security3);
        client.addSecurity(security4);

        var extractor = new EbasePDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0533033667"), hasWkn(null), hasTicker(null), //
                        hasName("MUL-LYX.MSCI Wd.IN.T.TR UC.ETF Nam.-An. ACC EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU2572257124"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi.I.S. MSCI World3 Act.Nom. U.ETF USD Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-17T00:00"), hasShares(0.2631950), //
                        hasSource("Umsatzabrechnung28.txt"), //
                        hasNote("Ref.-Nr.: 0400084351/14072023"), //
                        hasAmount("EUR", 150.00), hasGrossValue("EUR", 149.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.30))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-18T00:00"), hasShares(1.302784), //
                        hasSource("Umsatzabrechnung28.txt"), //
                        hasNote("Ref.-Nr.: 0400124334/14072023"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 99.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.20))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-01T00:00"), hasShares(4.563874), //
                        hasSource("Umsatzabrechnung28.txt"), //
                        hasNote("Ref.-Nr.: 0400000608/31072023"), //
                        hasAmount("EUR", 2000.00), hasGrossValue("EUR", 1996.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.99), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-01T00:00"), hasShares(18.81825900), //
                        hasSource("Umsatzabrechnung28.txt"), //
                        hasNote("Ref.-Nr.: 0400001107/31072023"), //
                        hasAmount("EUR", 2000.00), hasGrossValue("EUR", 1996.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.99), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testUmsatzabrechnung29()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung29.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2572257124"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi.I.S. MSCI World3 Act.Nom. U.ETF USD Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-27T00:00"), hasShares(0.533761), //
                        hasSource("Umsatzabrechnung29.txt"), //
                        hasNote("Ref.-Nr.: 0400004708/26102023 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-27T00:00"), hasShares(0.505318), //
                        hasSource("Umsatzabrechnung29.txt"), //
                        hasNote("Ref.-Nr.: 0400002625/24112023 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-19T00:00"), hasShares(0.604172), //
                        hasSource("Umsatzabrechnung29.txt"), //
                        hasNote("Ref.-Nr.: 0400130505/15122023 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 48.77), hasGrossValue("EUR", 48.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-15T00:00"), hasExDate(null), //
                        hasShares(39.68203200), //
                        hasSource("Umsatzabrechnung29.txt"), //
                        hasNote("Ref.-Nr.: 0400059203/15122023"), //
                        hasAmount("EUR", 48.77), hasGrossValue("EUR", 60.71), //
                        hasTaxes("EUR", 10.44 + 0.57 + 0.93), hasFees("EUR", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2023-12-15"), hasShares(39.682032), //
                                        hasSource("Umsatzabrechnung29.txt"), //
                                        hasNote("Ref.-Nr.: 0400059203/15122023"), //
                                        hasAmount("EUR", 48.77), hasGrossValue("EUR", 36.83), //
                                        hasTaxes("EUR", 11.94), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung30()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung30.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(2L));
        assertThat(results.size(), is(8));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0032077012"), hasWkn(null), hasTicker(null), //
                        hasName("InvescoMI3 NASDAQ100 ETF Registered Shares Dis o.N."), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00BYML9W36"), hasWkn(null), hasTicker(null), //
                        hasName("InvescoMI S&P 500 ETF Reg.Shares Dist o.N."), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-26T00:00"), hasShares(0.047550), //
                        hasSource("Umsatzabrechnung30.txt"), //
                        hasNote("Ref.-Nr.: 0400010911/22092023 | Wiederanlage Fondsertrag"), //
                        hasAmount("USD", 17.05), hasGrossValue("USD", 17.05), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-26T00:00"), hasShares(0.130878), //
                        hasSource("Umsatzabrechnung30.txt"), //
                        hasNote("Ref.-Nr.: 0400010223/22092023 | Wiederanlage Fondsertrag"), //
                        hasAmount("EUR", 4.96), hasGrossValue("EUR", 4.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-22T00:00"), hasExDate(null), //
                        hasShares(54.479737), //
                        hasSource("Umsatzabrechnung30.txt"), //
                        hasNote("Ref.-Nr.: 2210868091/45619741"), //
                        hasAmount("USD", 17.05), hasGrossValue("USD", 20.91), //
                        hasTaxes("USD", 3.86), hasFees("USD", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-22T00:00"), hasExDate(null), //
                        hasShares(38.873327), //
                        hasSource("Umsatzabrechnung30.txt"), //
                        hasNote("Ref.-Nr.: 0400008188/22092023"), //
                        hasAmount("EUR", 4.96), hasGrossValue("EUR", 6.08), //
                        hasForexGrossValue("USD", 6.50), //
                        hasTaxes("EUR", 1.07 + 0.05), hasFees("EUR", 0.00))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2023-09-22"), hasShares(54.479737), //
                                        hasSource("Umsatzabrechnung30.txt"), //
                                        hasNote("Ref.-Nr.: 2210868091/45619741"), //
                                        hasAmount("USD", 17.05), hasGrossValue("USD", 13.67), //
                                        hasTaxes("USD", 3.38), hasFees("USD", 0.00)))));

        // check skipped transaction
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2023-09-22"), hasShares(38.873327), //
                                        hasSource("Umsatzabrechnung30.txt"), //
                                        hasNote("Ref.-Nr.: 0400008188/22092023"), //
                                        hasAmount("EUR", 4.96), hasGrossValue("EUR", 3.84), //
                                        hasTaxes("EUR", 1.12), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmsatzabrechnung31()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung31.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(security( //
                        hasIsin("LU2023678282"), hasWkn(null), hasTicker(null), //
                        hasName("Lyx.Idx Fd-Dis.Tech.(DR) U.ETF Act. Nom. USD Acc. oN"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-14T00:00"), hasShares(3.395511), //
                        hasSource("Umsatzabrechnung31.txt"), //
                        hasNote("Ref.-Nr.: 0400001111/13072023 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-15T00:00"), hasShares(3.524447), //
                        hasSource("Umsatzabrechnung31.txt"), //
                        hasNote("Ref.-Nr.: 0400011111/14082023 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-15T00:00"), hasShares(3.508340), //
                        hasSource("Umsatzabrechnung31.txt"), //
                        hasNote("Ref.-Nr.: 0400001111/14092023 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));
    }

    @Test
    public void testUmsatzabrechnung32()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung32.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));

        assertThat(results, hasItem(security( //
                        hasIsin("LU0328476410"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.S&P Select Frontier Swap Inhaber-Anteile 1C o.N."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-04T00:00"), hasShares(2.959297), //
                        hasSource("Umsatzabrechnung32.txt"), //
                        hasNote("Ref.-Nr.: 400014734/03052021 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasForexGrossValue("USD", 47.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-31T00:00"), hasShares(2.885220), //
                        hasSource("Umsatzabrechnung32.txt"), //
                        hasNote("Ref.-Nr.: 400012542/28052021 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 39.92), //
                        hasForexGrossValue("USD", 48.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-06-18T00:00"), hasShares(2.043934), //
                        hasSource("Umsatzabrechnung32.txt"), //
                        hasNote("Ref.-Nr.: 708/17062021 | Ausgang externer Übertrag"), //
                        hasAmount("EUR", 28.64), hasGrossValue("EUR", 28.64), //
                        hasForexGrossValue("USD", 34.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2021-06-18T00:00"), hasShares(266), //
                        hasSource("Umsatzabrechnung32.txt"), //
                        hasNote("Ref.-Nr.: 708/17062021"), //
                        hasAmount("EUR", 3736.08), hasGrossValue("EUR", 3736.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-06-16T00:00"), //
                        hasShares(266), //
                        hasSource("Umsatzabrechnung32.txt"), //
                        hasNote("Ref.-Nr.: 708/17062021 | Depotführungsentgelt | VL-Vertragsentgelt"), //
                        hasAmount("EUR", 12.00 + 7.50), hasGrossValue("EUR", 12.00 + 7.50), //
                        hasForexGrossValue("USD", 23.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung33()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung33.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(10L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(14));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1854107148"), hasWkn(null), hasTicker(null), //
                        hasName("M&G(L)IF1-M&G(L) Posit. Impact Act. Nom. A EUR Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BNCB5M86"), hasWkn(null), hasTicker(null), //
                        hasName("Magna Umbre.Fd-M.New Frontiers Reg. Shares D Dis. EUR o.N"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-02T00:00"), hasShares(2.848289), //
                        hasSource("Umsatzabrechnung33.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-04T00:00"), hasShares(2.185315), //
                        hasSource("Umsatzabrechnung33.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-16T00:00"), hasShares(2.88344), //
                        hasSource("Umsatzabrechnung33.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-17T00:00"), hasShares(2.222486), //
                        hasSource("Umsatzabrechnung33.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-02T00:00"), hasShares(2.303581), //
                        hasSource("Umsatzabrechnung33.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-03T00:00"), hasShares(3.084795), //
                        hasSource("Umsatzabrechnung33.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-16T00:00"), hasShares(2.256182), //
                        hasSource("Umsatzabrechnung33.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-16T00:00"), hasShares(2.963818), //
                        hasSource("Umsatzabrechnung33.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-01T00:00"), hasShares(2.929550), //
                        hasSource("Umsatzabrechnung33.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-04T00:00"), hasShares(2.258084), //
                        hasSource("Umsatzabrechnung33.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2023-12-14T00:00"), hasShares(90.037945), //
                                        hasSource("Umsatzabrechnung33.txt"), //
                                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                                        hasAmount("EUR", 25.24), hasGrossValue("EUR", 25.24), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-14T00:00"), hasExDate(null), //
                        hasShares(90.037945), //
                        hasSource("Umsatzabrechnung33.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 25.24), hasGrossValue("EUR", 25.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung34()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung34.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1854107148"), hasWkn(null), hasTicker(null), //
                        hasName("M&G(L)IF1-M&G(L) Posit. Impact Act. Nom. A EUR Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BNCB5M86"), hasWkn(null), hasTicker(null), //
                        hasName("Magna Umbre.Fd-M.New Frontiers Reg. Shares D Dis. EUR o.N"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-02T00:00"), hasShares(2.569778), //
                        hasSource("Umsatzabrechnung34.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-03T00:00"), hasShares(1.961092), //
                        hasSource("Umsatzabrechnung34.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-15T00:00"), hasShares(2.587831), //
                        hasSource("Umsatzabrechnung34.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-16T00:00"), hasShares(1.990235), //
                        hasSource("Umsatzabrechnung34.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 37.50), hasGrossValue("EUR", 37.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        purchase( //
                                        hasDate("2024-04-29T00:00"), hasShares(128.804746), //
                                        hasSource("Umsatzabrechnung34.txt"), //
                                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                                        hasAmount("EUR", 22.73), hasGrossValue("EUR", 17.61), //
                                        hasTaxes("EUR", 4.48 + 0.24 + 0.40), hasFees("EUR", 0.00)))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-04-29T00:00"), hasExDate(null), //
                        hasShares(128.804746), //
                        hasSource("Umsatzabrechnung34.txt"), //
                        hasNote("Ref.-Nr.: 1234123412/12341234"), //
                        hasAmount("EUR", 22.73), hasGrossValue("EUR", 27.85), //
                        hasTaxes("EUR", 4.48 + 0.24 + 0.40), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzabrechnung35()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzabrechnung35.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));

        // The advance tax payment is always paid in local currency.
        // If the security is in foreign currency, the exchange rate is missing
        // in the document for posting.
        // new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK5BQT80"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE All-World U.ETF Reg. Shs USD Acc. oN"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-28T00:00"), hasShares(0.097258), //
                        hasSource("Umsatzabrechnung35.txt"), //
                        hasNote("Ref.-Nr.: 0400000987/27012025 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 13.29), hasGrossValue("EUR", 13.29), //
                        hasForexGrossValue("USD", 13.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-27T00:00"), hasShares(0.096250), //
                        hasSource("Umsatzabrechnung35.txt"), //
                        hasNote("Ref.-Nr.: 0400017713/26022025 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 13.29), hasGrossValue("EUR", 13.29), //
                        hasForexGrossValue("USD", 13.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-01T00:00"), hasShares(0.104955), //
                        hasSource("Umsatzabrechnung35.txt"), //
                        hasNote("Ref.-Nr.: 0400033849/31032025 | vermögenswirksame Leistungen"), //
                        hasAmount("EUR", 13.29), hasGrossValue("EUR", 13.26), //
                        hasForexGrossValue("USD", 14.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.03))));

        // check transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-24"), //
                        hasShares(0), //
                        hasSource("Umsatzabrechnung35.txt"), //
                        hasNote("Ref.-Nr.: 0400041153/24012025 | Vorabpauschale zum Stichtag 31.12.2024"), //
                        hasAmount("EUR", 1.18), hasGrossValue("EUR", 1.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmsatzuebersicht01()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umsatzuebersicht01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        deposit( //
                                        hasDate("2023-01-15T09:30"), hasAmount("EUR", 0.00), //
                                        hasSource("Umsatzuebersicht01.txt"), hasNote("Zeitraum: 01.01.2020 - 15.01.2023")))));
    }

    @Test
    public void testDepotStatement01()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotStatement01.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2022-12-12"), hasAmount("EUR", 200.00), //
                        hasSource("DepotStatement01.txt"), hasNote("SEPA Lastschrift Einzug | Ref.-Nr.: 020739500"))));
    }

    @Test
    public void testDepotstatement02()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotStatement02.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2020-04-14"), hasAmount("EUR", 15.00), //
                        hasSource("DepotStatement02.txt"), hasNote("SEPA Lastschrift Einzug | Ref.-Nr.: 123456"))));
    }

    @Test
    public void testDepotstatement03()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotStatement03.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2020-04-06"), hasAmount("EUR", 5000.00), //
                        hasSource("DepotStatement03.txt"), hasNote("SEPA Überweisung Gutschrift | Ref.-Nr.: 013015328"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-04-07"), hasAmount("EUR", 10000.00), //
                        hasSource("DepotStatement03.txt"), hasNote("SEPA Überweisung Gutschrift | Ref.-Nr.: 013017353"))));
    }

    @Test
    public void testDepotstatement04()
    {
        var extractor = new EbasePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotStatement04.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2021-06-21"), hasAmount("EUR", 1231.37), //
                        hasSource("DepotStatement04.txt"), hasNote("SEPA Überweisung | Ref.-Nr.: 014852180"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2021-06-21"), hasAmount("EUR", 2.50), //
                        hasSource("DepotStatement04.txt"), hasNote("Entgeltbuchung | Ref.-Nr.: 014852181"))));
    }
}
