package name.abuchen.portfolio.datatransfer.pdf.genobroker;

import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.GenoBrokerPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class GenoBrokerPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

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
                        hasIsin("FR001400IRI9"), hasWkn("A3EJEH"), hasTicker(null), //
                        hasName("Carbios SA Anrechte Aktie"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-30T09:57"), hasShares(30), //
                        hasSource("Kauf01.txt"), hasNote("Limit billigst"), //
                        hasAmount("EUR", 967.47), hasGrossValue("EUR", 926.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 32.95 + 5.60 + 2.52))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000FPWSL69"), hasWkn("WELT0B"), hasTicker(null), //
                        hasName("L&G-GERD KOMMER MUL.EQ.ETF REG.SHS USD ACC. ON"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-19T08:18"), hasShares(500), //
                        hasSource("Kauf02.txt"), hasNote("Auftragsnummer: 422576/44.00 | Limit 10,00 EUR"), //
                        hasAmount("EUR", 4714.55), hasGrossValue("EUR", 4704.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.95 + 0.10))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA5408991019"), hasWkn("A3EMQR"), hasTicker(null), //
                        hasName("LOGAN ENERGY CORP. REGISTERED SHARES O.N."), //
                        hasCurrencyCode("CAD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-03T12:00"), hasShares(2100), //
                        hasSource("Kauf03.txt"), hasNote("Auftragsnummer: 896962/04.00"), //
                        hasAmount("EUR", 506.62), hasGrossValue("EUR", 506.62), hasForexGrossValue("CAD", 735.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInEUR()
    {
        Security security = new Security("LOGAN ENERGY CORP. REGISTERED SHARES O.N.", CurrencyUnit.EUR);
        security.setIsin("CA5408991019");
        security.setWkn("A3EMQR");

        Client client = new Client();
        client.addSecurity(security);

        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-03T12:00"), hasShares(2100), //
                        hasSource("Kauf03.txt"), hasNote("Auftragsnummer: 896962/04.00"), //
                        hasAmount("EUR", 506.62), hasGrossValue("EUR", 506.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Status s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

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
                        hasIsin("CA84678A5089"), hasWkn("A3EHTZ"), hasTicker(null), //
                        hasName("SPARTAN DELTA CORP. REGISTERED SHARES NEW O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-07-25T16:48"), hasShares(2100), //
                        hasSource("Verkauf01.txt"), hasNote("Auftragsnummer: 433499/69.01 | Limit bestens"), //
                        hasAmount("EUR", 6319.37), hasGrossValue("EUR", 6331.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 12.03 + 0.10))));
    }

    @Test
    public void testDividende01()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

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
                        hasIsin("DE000A0LAUP1"), hasWkn("A0LAUP"), hasTicker(null), //
                        hasName("CROPENERGIES AG INHABER-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-07-14T00:00"), hasShares(1000), //
                        hasSource("Dividende01.txt"), hasNote("Abrechnungsnr.: 60007000"), //
                        hasAmount("EUR", 445.94), hasGrossValue("EUR", 615.87),//
                        hasTaxes("EUR", 15.87 + 146.03 + 8.03), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

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
                        hasIsin("CA84678A1021"), hasWkn("A2P5PY"), hasTicker(null), //
                        hasName("SPARTAN DELTA CORP. REGISTERED SHARES O.N."), //
                        hasCurrencyCode("CAD"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-07-06T00:00"), hasShares(2100), //
                        hasSource("Dividende02.txt"), hasNote("Abrechnungsnr.: 000000000"), //
                        hasAmount("EUR", 6107.09), hasGrossValue("EUR", 9475.70), hasForexGrossValue("CAD", 14133.00),//
                        hasTaxes("EUR", 2368.93 + 947.57 + 52.11), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFusion01()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fusion01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US69327R1014"), hasWkn("A1JZ02"), hasTicker(null), //
                        hasName("PDC ENERGY INC. REGISTERED SHARES DL -,01"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US1667641005"), hasWkn("852552"), hasTicker(null), //
                        hasName("CHEVRON CORP. REGISTERED SHARES DL-,75"), //
                        hasCurrencyCode("EUR"))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        inboundDelivery( //
                            hasDate("2023-08-07"), hasShares(23.19), //
                            hasSource("Fusion01.txt"), hasNote(null), //
                            hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                            hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                            hasDate("2023-08-07"), hasShares(50.00), //
                            hasSource("Fusion01.txt"), hasNote(null), //
                            hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                            hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }
}
