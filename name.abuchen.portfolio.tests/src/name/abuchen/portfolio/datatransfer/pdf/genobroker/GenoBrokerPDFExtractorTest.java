package name.abuchen.portfolio.datatransfer.pdf.genobroker;

import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.GenoBrokerPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
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
                        hasSource("Kauf01.txt"), hasNote("Auftragsnummer: 00000000 | Limit billigst"), //
                        hasAmount("EUR", 967.47), hasGrossValue("EUR", 926.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 32.95 + 5.60 + 2.52))));
    }

    @Test
    public void testDividende01()
    {
        GenoBrokerPDFExtractor extractor = new GenoBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

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
}
