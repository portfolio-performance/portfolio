package name.abuchen.portfolio.datatransfer.pdf.genobroker;

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

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.GenoBrokerPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
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
                        hasSource("Kauf01.txt"), //
                        hasNote("Limit billigst"), //
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
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer: 422576/44.00 | Limit 10,00 EUR"), //
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
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftragsnummer: 896962/04.00"), //
                        hasAmount("EUR", 506.62), hasGrossValue("EUR", 506.62), //
                        hasForexGrossValue("CAD", 735.00), //
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

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-03T12:00"), hasShares(2100), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftragsnummer: 896962/04.00"), //
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
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer: 433499/69.01 | Limit bestens"), //
                        hasAmount("EUR", 6319.37), hasGrossValue("EUR", 6331.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 12.03 + 0.10))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

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
                        hasIsin("IE000FPWSL69"), hasWkn("WELT0B"), hasTicker(null), //
                        hasName("L&G-GERD KOMMER MUL.EQ.ETF REG.SHS ETF USD DIS.ON"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-05T13:10"), hasShares(500), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftragsnummer: 498470/51.00 | Limit bestens"), //
                        hasAmount("EUR", 4759.31), hasGrossValue("EUR", 4779.50), //
                        hasTaxes("EUR", 9.61 + 0.53), hasFees("EUR", 9.95 + 0.10))));
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

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-07-14T00:00"), hasShares(1000), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abrechnungsnr.: 60007000"), //
                        hasAmount("EUR", 445.94), hasGrossValue("EUR", 615.87), //
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

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-07-10T00:00"), hasShares(2100), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungsnr.: 000000000"), //
                        hasAmount("EUR", 6107.09), hasGrossValue("EUR", 9475.70), //
                        hasForexGrossValue("CAD", 14133.00), //
                        hasTaxes("EUR", 2368.93 + 947.57 + 52.11), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NO0010096985"), hasWkn("675213"), hasTicker(null), //
                        hasName("EQUINOR ASA NAVNE-AKSJER NK 2,50"), //
                        hasCurrencyCode("NOK"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-08-29T00:00"), hasShares(600), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr.: 74014833940"), //
                        hasAmount("EUR", 236.34), hasGrossValue("EUR", 486.03), //
                        hasForexGrossValue("NOK", 5645.46), //
                        hasTaxes("EUR", 121.51 + 121.50 + 6.68), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        Security security = new Security("EQUINOR ASA NAVNE-AKSJER NK 2,50", CurrencyUnit.EUR);
        security.setIsin("NO0010096985");
        security.setWkn("675213");

        Client client = new Client();
        client.addSecurity(security);

        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-08-29T00:00"), hasShares(600), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr.: 74014833940"), //
                        hasAmount("EUR", 236.34), hasGrossValue("EUR", 486.03), //
                        hasTaxes("EUR", 121.51 + 121.50 + 6.68), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1667641005"), hasWkn("852552"), hasTicker(null), //
                        hasName("CHEVRON CORP. REGISTERED SHARES DL-,75"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-13T00:00"), hasShares(23), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr.: 75555439660"), //
                        hasAmount("EUR", 24.01), hasGrossValue("EUR", 32.26), //
                        hasForexGrossValue("USD", 34.73), //
                        hasTaxes("EUR", 4.84 + 3.23 + 0.18), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        Security security = new Security("EQUINOR ASA NAVNE-AKSJER NK 2,50", CurrencyUnit.USD);
        security.setIsin("US1667641005");
        security.setWkn("852552");

        Client client = new Client();
        client.addSecurity(security);

        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-13T00:00"), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr.: 75555439660"), //
                        hasAmount("EUR", 24.01), hasGrossValue("EUR", 32.26), //
                        hasTaxes("EUR", 4.84 + 3.23 + 0.18), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende05()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3XXRP09"), hasWkn("A1JX53"), hasTicker(null), //
                        hasName("VANGUARD S&P 500 UCITS ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-29T00:00"), hasShares(30), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Abrechnungsnr.: 86249245170"), //
                        hasAmount("EUR", 6.03), hasGrossValue("EUR", 7.50), //
                        hasForexGrossValue("USD", 8.38), //
                        hasTaxes("EUR", 1.29 + 0.07 + 0.11), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        Security security = new Security("VANGUARD S&P 500 UCITS ETF REGISTERED SHARES USD DIS.ON", CurrencyUnit.USD);
        security.setIsin("IE00B3XXRP09");
        security.setWkn("A1JX53");

        Client client = new Client();
        client.addSecurity(security);

        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-29T00:00"), hasShares(30), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Abrechnungsnr.: 86249245170"), //
                        hasAmount("EUR", 6.03), hasGrossValue("EUR", 7.50), //
                        hasTaxes("EUR", 1.29 + 0.07 + 0.11), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende06()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-04-30T00:00"), hasShares(75), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Abrechnungsnr.: 08172459718"), //
                        hasAmount("EUR", 255.00), hasGrossValue("EUR", 255.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008430026"), hasWkn("843002"), hasTicker(null), //
                        hasName("MUENCHENER RUECKVERS.-GES. AG VINK.NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-04-30T00:00"), hasShares(23), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Abrechnungsnr.: 20967773045"), //
                        hasAmount("EUR", 309.07), hasGrossValue("EUR", 345.00 + 216.60), //
                        hasTaxes("EUR", 31.39 + 1.72 + 2.82 + 216.60), hasFees("EUR", 0.00))));
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
                                        hasSource("Fusion01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check cancellation transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                                        hasDate("2023-08-07"), hasShares(50.00), //
                                        hasSource("Fusion01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }
}
