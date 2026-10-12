package name.abuchen.portfolio.datatransfer.pdf.dekabank;

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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.DekaBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class DekaBankPDFExtractorTest
{
    @Test
    public void testTagesauszug01()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug01.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("DE000DK0ECT0"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-UmweltInvest TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348461897"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-BioTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-18"), hasShares(21.303), //
                        hasSource("Tagesauszug01.txt"), hasNote("Auftragsnummer: 8103 1017"), //
                        hasAmount("EUR", 4000.00), hasGrossValue("EUR", 4000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-18"), hasShares(8.093), //
                        hasSource("Tagesauszug01.txt"), hasNote("Auftragsnummer: 8103 3945"), //
                        hasAmount("EUR", 4000.00), hasGrossValue("EUR", 4000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug02()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug02.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771824"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Liquidität: EURO TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0075131606"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Europa Nebenwerte TF (A)"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-06-06"), hasShares(5.112), //
                        hasSource("Tagesauszug02.txt"), hasNote("Auftragsnummer: 8102 2598"), //
                        hasAmount("EUR", 332.31), hasGrossValue("EUR", 332.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-06-06"), hasShares(3.181), //
                        hasSource("Tagesauszug02.txt"), hasNote("Auftragsnummer: 8102 3321"), //
                        hasAmount("EUR", 206.74), hasGrossValue("EUR", 206.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-06-06"), hasShares(2.598), //
                        hasSource("Tagesauszug02.txt"), hasNote("Auftragsnummer: 8102 2597"), //
                        hasAmount("EUR", 332.31), hasGrossValue("EUR", 334.13), //
                        hasTaxes("EUR", 1.82), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-06-06"), hasShares(2.579), //
                        hasSource("Tagesauszug02.txt"), hasNote("Auftragsnummer: 8102 3320"), //
                        hasAmount("EUR", 206.74), hasGrossValue("EUR", 208.46), //
                        hasTaxes("EUR", 1.72), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug03()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug03.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("LU0349172725"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-GlobalResources TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-14"), hasShares(28.939), //
                        hasSource("Tagesauszug03.txt"), hasNote("Auftragsnummer: 8101 4387"), //
                        hasAmount("EUR", 2355.09), hasGrossValue("EUR", 2376.18), //
                        hasTaxes("EUR", 21.09), hasFees("EUR", 0.00))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-14"), hasShares(20.647), //
                        hasSource("Tagesauszug03.txt"), hasNote("Auftragsnummer: 8102 3387"), //
                        hasAmount("EUR", 4000.00), hasGrossValue("EUR", 4000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug04()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug04.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(10L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ETFL227"), hasWkn(null), hasTicker(null), //
                        hasName("Deka Deutsche Börse EUROGOV Germany Money Market UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-09"), hasShares(0.597), //
                        hasSource("Tagesauszug04.txt"), hasNote("Auftragsnummer: 9704 9385"), //
                        hasAmount("EUR", 41.33), hasGrossValue("EUR", 41.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-09"), hasShares(0.425), //
                        hasSource("Tagesauszug04.txt"), hasNote("Auftragsnummer: 9705 0130"), //
                        hasAmount("EUR", 29.42), hasGrossValue("EUR", 29.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-09"), hasShares(0.551), //
                        hasSource("Tagesauszug04.txt"), hasNote("Auftragsnummer: 9705 1035"), //
                        hasAmount("EUR", 38.12), hasGrossValue("EUR", 38.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-09"), hasShares(0.316), //
                        hasSource("Tagesauszug04.txt"), hasNote("Auftragsnummer: 9705 1612"), //
                        hasAmount("EUR", 21.89), hasGrossValue("EUR", 21.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-09"), hasShares(0.220), //
                        hasSource("Tagesauszug04.txt"), hasNote("Auftragsnummer: 9705 3372"), //
                        hasAmount("EUR", 15.25), hasGrossValue("EUR", 15.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-09"), hasShares(0.324), //
                        hasSource("Tagesauszug04.txt"), hasNote("Auftragsnummer: 9705 4016"), //
                        hasAmount("EUR", 22.45), hasGrossValue("EUR", 22.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-09"), hasShares(0.362), //
                        hasSource("Tagesauszug04.txt"), hasNote("Auftragsnummer: 9705 4607"), //
                        hasAmount("EUR", 25.02), hasGrossValue("EUR", 25.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-09"), hasShares(0.215), //
                        hasSource("Tagesauszug04.txt"), hasNote("Auftragsnummer: 9705 5155"), //
                        hasAmount("EUR", 14.85), hasGrossValue("EUR", 14.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-09"), hasShares(0.360), //
                        hasSource("Tagesauszug04.txt"), hasNote("Auftragsnummer: 9705 6066"), //
                        hasAmount("EUR", 24.92), hasGrossValue("EUR", 24.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-09"), hasShares(0.295), //
                        hasSource("Tagesauszug04.txt"), hasNote("Auftragsnummer: 9705 6630"), //
                        hasAmount("EUR", 20.40), hasGrossValue("EUR", 20.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug05()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug05.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("LU1225718409"), hasWkn(null), hasTicker(null), //
                        hasName("Partners Group List. Investm. SICAV-Listed Infrastructure C"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1508359509"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Industrie 4.0 CF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-04"), hasShares(0.369), //
                        hasSource("Tagesauszug05.txt"), hasNote("Auftragsnummer: 9701 0301"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-03-07"), hasShares(0.083), //
                        hasSource("Tagesauszug05.txt"), hasNote("Auftragsnummer: 8108 0595"), //
                        hasAmount("EUR", 14.74), hasGrossValue("EUR", 14.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug06()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug06.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(13L));
        assertThat(countBuySell(results), is(15L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(30));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B66F4759"), hasWkn(null), hasTicker(null), //
                        hasName("iShares EUR High Yield Corp Bond UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B9M6RS56"), hasWkn(null), hasTicker(null), //
                        hasName("iShares J.P. Morgan USD EM Bond EUR Hedged UCITS ETF (Dist)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010296061"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor MSCI USA UCITS ETF D-EUR"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0302447452"), hasWkn(null), hasTicker(null), //
                        hasName("Schroder ISF Global Climate Change Equity C"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B44CGS96"), hasWkn(null), hasTicker(null), //
                        hasName("iShares US Aggregate Bond UCITS ETF"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1602144732"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi Index MSCI Japan UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0703710904"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Nachhaltigkeit Aktien CF (A)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1508359509"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Industrie 4.0 CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ETFL144"), hasWkn(null), hasTicker(null), //
                        hasName("Deka iBoxx EUR Liquid Sovereign Diversified 5-7 UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ETFL284"), hasWkn(null), hasTicker(null), //
                        hasName("Deka MSCI Europe UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ETFL375"), hasWkn(null), hasTicker(null), //
                        hasName("Deka iBoxx EUR Liquid Corporates Diversified UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1275255799"), hasWkn(null), hasTicker(null), //
                        hasName("ComStage CBK Commodity ex-Agri. Monthly EUR Hdg UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010429068"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor MSCI Emerging Markets UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.010), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9711 1533"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.002), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9715 5001"), //
                        hasAmount("EUR", 0.14), hasGrossValue("EUR", 0.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.018), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9703 5128"), //
                        hasAmount("EUR", 6.98), hasGrossValue("EUR", 6.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.071), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9705 6371"), //
                        hasAmount("EUR", 2.35), hasGrossValue("EUR", 2.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.052), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9709 6969"), //
                        hasAmount("EUR", 4.98), hasGrossValue("EUR", 4.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.067), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9712 7274"), //
                        hasAmount("EUR", 6.22), hasGrossValue("EUR", 6.22), //
                        hasForexGrossValue("USD", 6.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.047), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9714 2092"), //
                        hasAmount("EUR", 3.66), hasGrossValue("EUR", 3.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.012), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9718 1490"), //
                        hasAmount("EUR", 2.70), hasGrossValue("EUR", 2.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-04"), hasShares(0.010), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9733 9061"), //
                        hasAmount("EUR", 2.56), hasGrossValue("EUR", 2.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-04"), hasShares(0.013), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9734 0025"), //
                        hasAmount("EUR", 2.42), hasGrossValue("EUR", 2.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.034), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9721 1901"), //
                        hasAmount("EUR", 3.73), hasGrossValue("EUR", 3.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.282), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9723 4163"), //
                        hasAmount("EUR", 4.35), hasGrossValue("EUR", 4.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.024), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9724 3146"), //
                        hasAmount("EUR", 2.56), hasGrossValue("EUR", 2.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.036), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9727 4488"), //
                        hasAmount("EUR", 3.80), hasGrossValue("EUR", 3.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.287), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9728 1854"), //
                        hasAmount("EUR", 3.46), hasGrossValue("EUR", 3.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-31"), hasExDate(null), //
                        hasShares(0.655), //
                        hasSource("Tagesauszug06.txt"), //
                        hasNote("Auftragsnummer: 9301 6001"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-31"), hasExDate(null), //
                        hasShares(0.619), //
                        hasSource("Tagesauszug06.txt"), //
                        hasNote("Auftragsnummer: 9302 2538"), //
                        hasAmount("EUR", 0.14), hasGrossValue("EUR", 0.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug06WithSecurityInUSD()
    {
        var security5 = new Security("iShares US Aggregate Bond UCITS ETF", "EUR");
        security5.setIsin("IE00B44CGS96");

        var client = new Client();
        client.addSecurity(security5);

        var extractor = new DekaBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug06.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(12L));
        assertThat(countBuySell(results), is(15L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(29));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B66F4759"), hasWkn(null), hasTicker(null), //
                        hasName("iShares EUR High Yield Corp Bond UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B9M6RS56"), hasWkn(null), hasTicker(null), //
                        hasName("iShares J.P. Morgan USD EM Bond EUR Hedged UCITS ETF (Dist)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010296061"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor MSCI USA UCITS ETF D-EUR"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0302447452"), hasWkn(null), hasTicker(null), //
                        hasName("Schroder ISF Global Climate Change Equity C"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1602144732"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi Index MSCI Japan UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0703710904"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Nachhaltigkeit Aktien CF (A)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1508359509"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Industrie 4.0 CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ETFL144"), hasWkn(null), hasTicker(null), //
                        hasName("Deka iBoxx EUR Liquid Sovereign Diversified 5-7 UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ETFL284"), hasWkn(null), hasTicker(null), //
                        hasName("Deka MSCI Europe UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ETFL375"), hasWkn(null), hasTicker(null), //
                        hasName("Deka iBoxx EUR Liquid Corporates Diversified UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1275255799"), hasWkn(null), hasTicker(null), //
                        hasName("ComStage CBK Commodity ex-Agri. Monthly EUR Hdg UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010429068"), hasWkn(null), hasTicker(null), //
                        hasName("Lyxor MSCI Emerging Markets UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.010), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9711 1533"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.002), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9715 5001"), //
                        hasAmount("EUR", 0.14), hasGrossValue("EUR", 0.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.018), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9703 5128"), //
                        hasAmount("EUR", 6.98), hasGrossValue("EUR", 6.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.071), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9705 6371"), //
                        hasAmount("EUR", 2.35), hasGrossValue("EUR", 2.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.052), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9709 6969"), //
                        hasAmount("EUR", 4.98), hasGrossValue("EUR", 4.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.067), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9712 7274"), //
                        hasAmount("EUR", 6.22), hasGrossValue("EUR", 6.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.047), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9714 2092"), //
                        hasAmount("EUR", 3.66), hasGrossValue("EUR", 3.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.012), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9718 1490"), //
                        hasAmount("EUR", 2.70), hasGrossValue("EUR", 2.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-04"), hasShares(0.010), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9733 9061"), //
                        hasAmount("EUR", 2.56), hasGrossValue("EUR", 2.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-04"), hasShares(0.013), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9734 0025"), //
                        hasAmount("EUR", 2.42), hasGrossValue("EUR", 2.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.034), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9721 1901"), //
                        hasAmount("EUR", 3.73), hasGrossValue("EUR", 3.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.282), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9723 4163"), //
                        hasAmount("EUR", 4.35), hasGrossValue("EUR", 4.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.024), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9724 3146"), //
                        hasAmount("EUR", 2.56), hasGrossValue("EUR", 2.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.036), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9727 4488"), //
                        hasAmount("EUR", 3.80), hasGrossValue("EUR", 3.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01"), hasShares(0.287), //
                        hasSource("Tagesauszug06.txt"), hasNote("Auftragsnummer: 9728 1854"), //
                        hasAmount("EUR", 3.46), hasGrossValue("EUR", 3.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-31"), hasExDate(null), //
                        hasShares(0.655), //
                        hasSource("Tagesauszug06.txt"), //
                        hasNote("Auftragsnummer: 9301 6001"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-31"), hasExDate(null), //
                        hasShares(0.619), //
                        hasSource("Tagesauszug06.txt"), //
                        hasNote("Auftragsnummer: 9302 2538"), //
                        hasAmount("EUR", 0.14), hasGrossValue("EUR", 0.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug07()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug07.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-02-24"), hasExDate(null), //
                        hasShares(33.823), //
                        hasSource("Tagesauszug07.txt"), //
                        hasNote("Auftragsnummer: 9457 2850"), //
                        hasAmount("EUR", 63.47), hasGrossValue("EUR", 78.81), //
                        hasTaxes("EUR", 15.34), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-02-24"), hasExDate(null), //
                        hasShares(8.848), //
                        hasSource("Tagesauszug07.txt"), //
                        hasNote("Auftragsnummer: 9457 2851"), //
                        hasAmount("EUR", 16.61), hasGrossValue("EUR", 20.62), //
                        hasTaxes("EUR", 4.01), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-24"), hasShares(0.457), //
                        hasSource("Tagesauszug07.txt"), hasNote("Auftragsnummer: 9457 2850 | Wiederanlage"), //
                        hasAmount("EUR", 63.47), hasGrossValue("EUR", 63.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-24"), hasShares(0.120), //
                        hasSource("Tagesauszug07.txt"), hasNote("Auftragsnummer: 9457 2851 | Wiederanlage"), //
                        hasAmount("EUR", 16.61), hasGrossValue("EUR", 16.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug08()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug08.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("LU0268059614"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GeldmarktPlan TF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-10-02"), hasShares(0.402), //
                        hasSource("Tagesauszug08.txt"), hasNote("Auftragsnummer: 8101 2521"), //
                        hasAmount("EUR", 400.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug09()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug09.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("LU0268059614"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GeldmarktPlan TF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-03-06"), hasShares(1.373), //
                        hasSource("Tagesauszug09.txt"), hasNote("Auftragsnummer: 8101 3955"), //
                        hasAmount("EUR", 1400.00), hasGrossValue("EUR", 1400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug10()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug10.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("LU0268059614"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GeldmarktPlan TF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-06-16"), hasShares(0.977), //
                        hasSource("Tagesauszug10.txt"), hasNote("Auftragsnummer: 8101 3074"), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug11()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug11.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka CF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-12-14"), hasShares(1.339), //
                        hasSource("Tagesauszug11.txt"), hasNote("Auftragsnummer: 7103 3775"), //
                        hasAmount("EUR", 72.00), hasGrossValue("EUR", 69.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.69))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-12-14"), hasShares(0.465), //
                        hasSource("Tagesauszug11.txt"), hasNote("Auftragsnummer: 7104 1292"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 22.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.69))));
    }

    @Test
    public void testTagesauszug12()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug12.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-02-20"), hasExDate(null), //
                        hasShares(29.811), //
                        hasSource("Tagesauszug12.txt"), //
                        hasNote("Auftragsnummer: 9387 9103"), //
                        hasAmount("EUR", 27.72), hasGrossValue("EUR", 27.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-02-20"), hasExDate(null), //
                        hasShares(35.950), //
                        hasSource("Tagesauszug12.txt"), //
                        hasNote("Auftragsnummer: 9401 7546"), //
                        hasAmount("EUR", 3.60), hasGrossValue("EUR", 3.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2009-02-20"), //
                        hasShares(29.811), //
                        hasSource("Tagesauszug12.txt"), //
                        hasNote("Auftragsnummer: 9387 9103"), //
                        hasAmount("EUR", 1.43), hasGrossValue("EUR", 1.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-02-20"), hasShares(0.872), //
                        hasSource("Tagesauszug12.txt"), hasNote("Auftragsnummer: 9387 9103 | Wiederanlage"), //
                        hasAmount("EUR", 29.15), hasGrossValue("EUR", 29.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-02-20"), hasShares(0.095), //
                        hasSource("Tagesauszug12.txt"), hasNote("Auftragsnummer: 9401 7546 | Wiederanlage"), //
                        hasAmount("EUR", 3.60), hasGrossValue("EUR", 3.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug13()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug13.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0268059614"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GeldmarktPlan TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771980"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaBond TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-08-20"), hasExDate(null), //
                        hasShares(1.565), //
                        hasSource("Tagesauszug13.txt"), //
                        hasNote("Auftragsnummer: 9311 0573"), //
                        hasAmount("EUR", 46.31), hasGrossValue("EUR", 46.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-08-20"), hasExDate(null), //
                        hasShares(49.088), //
                        hasSource("Tagesauszug13.txt"), //
                        hasNote("Auftragsnummer: 9329 3756"), //
                        hasAmount("EUR", 51.54), hasGrossValue("EUR", 51.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-08-20"), hasShares(0.047), //
                        hasSource("Tagesauszug13.txt"), hasNote("Auftragsnummer: 9311 0573 | Wiederanlage"), //
                        hasAmount("EUR", 46.31), hasGrossValue("EUR", 46.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-08-20"), hasShares(1.418), //
                        hasSource("Tagesauszug13.txt"), hasNote("Auftragsnummer: 9329 3756 | Wiederanlage"), //
                        hasAmount("EUR", 51.54), hasGrossValue("EUR", 51.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug14()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug14.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-08-20"), hasExDate(null), //
                        hasShares(15.795), //
                        hasSource("Tagesauszug14.txt"), //
                        hasNote("Auftragsnummer: 9310 0970"), //
                        hasAmount("EUR", 105.19), hasGrossValue("EUR", 105.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-08-20"), hasShares(0.385), //
                        hasSource("Tagesauszug14.txt"), hasNote("Auftragsnummer: 9310 0970 | Wiederanlage"), //
                        hasAmount("EUR", 105.19), hasGrossValue("EUR", 105.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug15()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug15.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2012-11-20"), hasExDate(null), //
                        hasShares(6.190), //
                        hasSource("Tagesauszug15.txt"), //
                        hasNote("Auftragsnummer: 9307 0124"), //
                        hasAmount("EUR", 58.50), hasGrossValue("EUR", 58.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-11-20"), hasShares(0.134), //
                        hasSource("Tagesauszug15.txt"), hasNote("Auftragsnummer: 9307 0124 | Wiederanlage"), //
                        hasAmount("EUR", 58.50), hasGrossValue("EUR", 58.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug16()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug16.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka CF"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-02-21"), hasExDate(null), //
                        hasShares(34.746), //
                        hasSource("Tagesauszug16.txt"), //
                        hasNote("Auftragsnummer: 9398 1226"), //
                        hasAmount("EUR", 27.10), hasGrossValue("EUR", 27.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2014-02-21"), hasShares(0.451), //
                        hasSource("Tagesauszug16.txt"), hasNote("Auftragsnummer: 9398 1226 | Wiederanlage"), //
                        hasAmount("EUR", 27.10), hasGrossValue("EUR", 27.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug17()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug17.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-11-22"), hasExDate(null), //
                        hasShares(6.032), //
                        hasSource("Tagesauszug17.txt"), //
                        hasNote("Auftragsnummer: 9306 3038"), //
                        hasAmount("EUR", 27.51), hasGrossValue("EUR", 27.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-11-22"), hasShares(0.058), //
                        hasSource("Tagesauszug17.txt"), hasNote("Auftragsnummer: 9306 3038 | Wiederanlage"), //
                        hasAmount("EUR", 27.51), hasGrossValue("EUR", 27.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug18()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug18.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0268059614"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-LiquiditätsPlan TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771980"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaBond TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-08-19"), hasExDate(null), //
                        hasShares(1.023), //
                        hasSource("Tagesauszug18.txt"), //
                        hasNote("Auftragsnummer: 9309 1751"), //
                        hasAmount("EUR", 12.48), hasGrossValue("EUR", 12.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-08-19"), hasExDate(null), //
                        hasShares(52.202), //
                        hasSource("Tagesauszug18.txt"), //
                        hasNote("Auftragsnummer: 9322 8046"), //
                        hasAmount("EUR", 60.03), hasGrossValue("EUR", 60.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-08-19"), hasShares(0.013), //
                        hasSource("Tagesauszug18.txt"), hasNote("Auftragsnummer: 9309 1751 | Wiederanlage"), //
                        hasAmount("EUR", 12.48), hasGrossValue("EUR", 12.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-08-19"), hasShares(1.580), //
                        hasSource("Tagesauszug18.txt"), hasNote("Auftragsnummer: 9322 8046 | Wiederanlage"), //
                        hasAmount("EUR", 60.03), hasGrossValue("EUR", 60.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug19()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug19.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLuxTeam-Aktien Asien"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-11-19"), hasExDate(null), //
                        hasShares(15.279), //
                        hasSource("Tagesauszug19.txt"), //
                        hasNote("Auftragsnummer: 9410 2301"), //
                        hasAmount("EUR", 14.97), hasGrossValue("EUR", 14.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-11-19"), hasExDate(null), //
                        hasShares(5.888), //
                        hasSource("Tagesauszug19.txt"), //
                        hasNote("Auftragsnummer: 9410 2302"), //
                        hasAmount("EUR", 5.77), hasGrossValue("EUR", 5.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-11-19"), hasExDate(null), //
                        hasShares(6.558), //
                        hasSource("Tagesauszug19.txt"), //
                        hasNote("Auftragsnummer: 9412 4474"), //
                        hasAmount("EUR", 38.76), hasGrossValue("EUR", 38.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-11-19"), hasExDate(null), //
                        hasShares(33.486), //
                        hasSource("Tagesauszug19.txt"), //
                        hasNote("Auftragsnummer: 9413 5783"), //
                        hasAmount("EUR", 54.25), hasGrossValue("EUR", 54.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-11-19"), hasShares(0.122), //
                        hasSource("Tagesauszug19.txt"), hasNote("Auftragsnummer: 9410 2301 | Wiederanlage"), //
                        hasAmount("EUR", 14.97), hasGrossValue("EUR", 14.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-11-19"), hasShares(0.047), //
                        hasSource("Tagesauszug19.txt"), hasNote("Auftragsnummer: 9410 2302 | Wiederanlage"), //
                        hasAmount("EUR", 5.77), hasGrossValue("EUR", 5.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-11-19"), hasShares(0.073), //
                        hasSource("Tagesauszug19.txt"), hasNote("Auftragsnummer: 9412 4474 | Wiederanlage"), //
                        hasAmount("EUR", 38.76), hasGrossValue("EUR", 38.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-11-19"), hasShares(0.457), //
                        hasSource("Tagesauszug19.txt"), hasNote("Auftragsnummer: 9413 5783 | Wiederanlage"), //
                        hasAmount("EUR", 54.25), hasGrossValue("EUR", 54.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug20()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug20.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-02-20"), hasExDate(null), //
                        hasShares(35.197), //
                        hasSource("Tagesauszug20.txt"), //
                        hasNote("Auftragsnummer: 9401 1742"), //
                        hasAmount("EUR", 77.43), hasGrossValue("EUR", 77.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-02-20"), hasExDate(null), //
                        hasShares(36.045), //
                        hasSource("Tagesauszug20.txt"), //
                        hasNote("Auftragsnummer: 9426 3111"), //
                        hasAmount("EUR", 11.89), hasGrossValue("EUR", 11.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-02-20"), hasShares(1.135), //
                        hasSource("Tagesauszug20.txt"), hasNote("Auftragsnummer: 9401 1742 | Wiederanlage"), //
                        hasAmount("EUR", 77.43), hasGrossValue("EUR", 77.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-02-20"), hasShares(0.118), //
                        hasSource("Tagesauszug20.txt"), hasNote("Auftragsnummer: 9426 3111 | Wiederanlage"), //
                        hasAmount("EUR", 11.89), hasGrossValue("EUR", 11.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug21()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug21.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0268059614"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GeldmarktPlan TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771980"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaBond TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-08-20"), hasExDate(null), //
                        hasShares(1.013), //
                        hasSource("Tagesauszug21.txt"), //
                        hasNote("Auftragsnummer: 9309 2789"), //
                        hasAmount("EUR", 9.93), hasGrossValue("EUR", 9.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-08-20"), hasExDate(null), //
                        hasShares(50.506), //
                        hasSource("Tagesauszug21.txt"), //
                        hasNote("Auftragsnummer: 9324 0160"), //
                        hasAmount("EUR", 66.16), hasGrossValue("EUR", 66.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-08-20"), hasShares(0.010), //
                        hasSource("Tagesauszug21.txt"), hasNote("Auftragsnummer: 9309 2789 | Wiederanlage"), //
                        hasAmount("EUR", 9.93), hasGrossValue("EUR", 9.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-08-20"), hasShares(1.696), //
                        hasSource("Tagesauszug21.txt"), hasNote("Auftragsnummer: 9324 0160 | Wiederanlage"), //
                        hasAmount("EUR", 66.16), hasGrossValue("EUR", 66.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug22()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug22.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-11-20"), hasExDate(null), //
                        hasShares(5.934), //
                        hasSource("Tagesauszug22.txt"), //
                        hasNote("Auftragsnummer: 9308 3980"), //
                        hasAmount("EUR", 38.27), hasGrossValue("EUR", 38.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-11-20"), hasExDate(null), //
                        hasShares(32.324), //
                        hasSource("Tagesauszug22.txt"), //
                        hasNote("Auftragsnummer: 9310 4730"), //
                        hasAmount("EUR", 3.23), hasGrossValue("EUR", 3.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-11-20"), hasShares(0.098), //
                        hasSource("Tagesauszug22.txt"), hasNote("Auftragsnummer: 9308 3980 | Wiederanlage"), //
                        hasAmount("EUR", 38.27), hasGrossValue("EUR", 38.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-11-20"), hasShares(0.047), //
                        hasSource("Tagesauszug22.txt"), hasNote("Auftragsnummer: 9310 4730 | Wiederanlage"), //
                        hasAmount("EUR", 3.23), hasGrossValue("EUR", 3.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug23()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug23.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2013-11-20"), hasExDate(null), //
                        hasShares(6.324), //
                        hasSource("Tagesauszug23.txt"), //
                        hasNote("Auftragsnummer: 9405 8250"), //
                        hasAmount("EUR", 56.73), hasGrossValue("EUR", 56.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2013-11-20"), hasExDate(null), //
                        hasShares(32.445), //
                        hasSource("Tagesauszug23.txt"), //
                        hasNote("Auftragsnummer: 9406 3275"), //
                        hasAmount("EUR", 52.89), hasGrossValue("EUR", 52.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2013-11-20"), hasShares(0.125), //
                        hasSource("Tagesauszug23.txt"), hasNote("Auftragsnummer: 9405 8250 | Wiederanlage"), //
                        hasAmount("EUR", 56.73), hasGrossValue("EUR", 56.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2013-11-20"), hasShares(0.517), //
                        hasSource("Tagesauszug23.txt"), hasNote("Auftragsnummer: 9406 3275 | Wiederanlage"), //
                        hasAmount("EUR", 52.89), hasGrossValue("EUR", 52.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug24()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug24.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLuxTeam-Aktien Asien"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-11-20"), hasExDate(null), //
                        hasShares(15.216), //
                        hasSource("Tagesauszug24.txt"), //
                        hasNote("Auftragsnummer: 9406 3036"), //
                        hasAmount("EUR", 8.22), hasGrossValue("EUR", 8.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-11-20"), hasExDate(null), //
                        hasShares(5.864), //
                        hasSource("Tagesauszug24.txt"), //
                        hasNote("Auftragsnummer: 9406 3037"), //
                        hasAmount("EUR", 3.17), hasGrossValue("EUR", 3.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-11-20"), hasExDate(null), //
                        hasShares(6.449), //
                        hasSource("Tagesauszug24.txt"), //
                        hasNote("Auftragsnummer: 9407 8611"), //
                        hasAmount("EUR", 53.14), hasGrossValue("EUR", 53.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-11-20"), hasExDate(null), //
                        hasShares(32.962), //
                        hasSource("Tagesauszug24.txt"), //
                        hasNote("Auftragsnummer: 9408 5425"), //
                        hasAmount("EUR", 52.74), hasGrossValue("EUR", 52.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2014-11-20"), hasShares(0.063), //
                        hasSource("Tagesauszug24.txt"), hasNote("Auftragsnummer: 9406 3036 | Wiederanlage"), //
                        hasAmount("EUR", 8.22), hasGrossValue("EUR", 8.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2014-11-20"), hasShares(0.024), //
                        hasSource("Tagesauszug24.txt"), hasNote("Auftragsnummer: 9406 3037 | Wiederanlage"), //
                        hasAmount("EUR", 3.17), hasGrossValue("EUR", 3.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2014-11-20"), hasShares(0.109), //
                        hasSource("Tagesauszug24.txt"), hasNote("Auftragsnummer: 9407 8611 | Wiederanlage"), //
                        hasAmount("EUR", 53.14), hasGrossValue("EUR", 53.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2014-11-20"), hasShares(0.524), //
                        hasSource("Tagesauszug24.txt"), hasNote("Auftragsnummer: 9408 5425 | Wiederanlage"), //
                        hasAmount("EUR", 52.74), hasGrossValue("EUR", 52.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug25()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug25.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-11-21"), hasExDate(null), //
                        hasShares(6.090), //
                        hasSource("Tagesauszug25.txt"), //
                        hasNote("Auftragsnummer: 9404 4323"), //
                        hasAmount("EUR", 38.73), hasGrossValue("EUR", 38.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-11-21"), hasExDate(null), //
                        hasShares(32.371), //
                        hasSource("Tagesauszug25.txt"), //
                        hasNote("Auftragsnummer: 9405 0501"), //
                        hasAmount("EUR", 4.86), hasGrossValue("EUR", 4.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-11-21"), hasShares(0.100), //
                        hasSource("Tagesauszug25.txt"), hasNote("Auftragsnummer: 9404 4323 | Wiederanlage"), //
                        hasAmount("EUR", 38.73), hasGrossValue("EUR", 38.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd reinvest transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-11-21"), hasShares(0.074), //
                        hasSource("Tagesauszug25.txt"), hasNote("Auftragsnummer: 9405 0501 | Wiederanlage"), //
                        hasAmount("EUR", 4.86), hasGrossValue("EUR", 4.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesauszug26()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug26.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(6L));
        assertThat(countBuySell(results), is(7L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(7L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLuxTeam-Aktien Asien"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-19"), hasShares(0.882), //
                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8101 8358"), //
                        hasAmount("EUR", 232.27), hasGrossValue("EUR", 232.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-18"), hasShares(0.388), //
                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8102 9563"), //
                        hasAmount("EUR", 18.49), hasGrossValue("EUR", 18.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-18"), hasShares(0.952), //
                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8103 0561"), //
                        hasAmount("EUR", 110.18), hasGrossValue("EUR", 110.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-18"), hasShares(0.148), //
                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8103 0563"), //
                        hasAmount("EUR", 17.13), hasGrossValue("EUR", 17.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-19"), hasShares(0.854), //
                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8103 0609"), //
                        hasAmount("EUR", 494.76), hasGrossValue("EUR", 494.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-18"), hasShares(0.726), //
                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8103 1685"), //
                        hasAmount("EUR", 58.09), hasGrossValue("EUR", 58.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-18"), hasShares(0.901), //
                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8103 3824"), //
                        hasAmount("EUR", 79.92), hasGrossValue("EUR", 79.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(16), //
                                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8101 8357"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(40), //
                                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8102 9562"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(15), //
                                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8103 0560"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(6), //
                                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8103 0562"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(6), //
                                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8103 0608"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(34), //
                                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8103 1684"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(37), //
                                        hasSource("Tagesauszug26.txt"), hasNote("Auftragsnummer: 8103 3823"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testTagesauszug27()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Tagesauszug27.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0268059614"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GeldmarktPlan TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-04-16"), hasShares(5.864), //
                        hasSource("Tagesauszug27.txt"), hasNote("Auftragsnummer: 8101 2364"), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2010-04-16"), hasShares(1.001), //
                        hasSource("Tagesauszug27.txt"), hasNote("Auftragsnummer: 8101 2363"), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht01()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht01.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(11L));
        assertThat(countBuySell(results), is(67L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(80));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK0ECT0"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-UmweltInvest TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK0ECV6"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GlobalChampions TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK0EC91"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Sachwerte TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348461897"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-BioTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0349172725"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-GlobalResources TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1508360002"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Industrie 4.0 TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1496713741"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Europa Nebenwerte CF (A)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0064405334"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-USA TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0075131606"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Europa Nebenwerte TF (A)"), //
                        hasCurrencyCode("EUR"))));

        // check 1st security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(1.258), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-15"), hasShares(1.253), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(1.269), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-17"), hasShares(1.335), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-18"), hasShares(21.303), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 4000.00), hasGrossValue("EUR", 4000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(1.286), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-15"), hasShares(1.275), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(1.087), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-04-01"), hasShares(6.736), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 1549.55), hasGrossValue("EUR", 1549.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-15"), hasShares(1.075), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(1.068), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-17"), hasShares(1.082), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(1.068), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-15"), hasShares(1.037), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-06-25"), hasShares(5.330), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 1310.43), hasGrossValue("EUR", 1310.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(2.451), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-15"), hasShares(2.439), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(2.433), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-05-11"), hasShares(19.842), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 2039.96), hasGrossValue("EUR", 2039.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-17"), hasShares(2.431), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(2.426), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-15"), hasShares(2.426), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(0.735), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-15"), hasShares(0.739), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(0.730), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-05-12"), hasShares(5.230), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 1796.19), hasGrossValue("EUR", 1796.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-17"), hasShares(0.724), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(0.725), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-15"), hasShares(0.704), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-06-15"), hasShares(2.153), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 764.21), hasGrossValue("EUR", 764.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(0.485), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-15"), hasShares(0.492), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(0.483), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-17"), hasShares(0.505), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-18"), hasShares(8.093), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 4000.00), hasGrossValue("EUR", 4000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(0.502), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-15"), hasShares(0.466), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-06-16"), hasShares(12.416), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 6556.02), hasGrossValue("EUR", 6556.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(3.395), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-15"), hasShares(3.340), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(3.337), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-17"), hasShares(3.157), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-05-17"), hasShares(16.535), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 1309.24), hasGrossValue("EUR", 1309.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(3.145), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-15"), hasShares(3.094), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(1.290), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-15"), hasShares(1.273), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(1.301), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-17"), hasShares(1.382), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(1.328), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-15"), hasShares(1.298), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-06-22"), hasShares(10.488), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 2044.85), hasGrossValue("EUR", 2044.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(1.267), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-17"), hasShares(1.319), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(1.510), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-15"), hasShares(1.534), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-03"), hasShares(1.533), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-05-14"), hasShares(13.620), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 2314.45), hasGrossValue("EUR", 2314.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-17"), hasShares(1.465), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-01"), hasShares(1.404), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-06-08"), hasShares(2.869), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 521.87), hasGrossValue("EUR", 521.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-15"), hasShares(1.361), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-07"), hasShares(1.464), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-15"), hasShares(1.452), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-01"), hasShares(2.217), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-04-12"), hasShares(13.391), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 1540.37), hasGrossValue("EUR", 1540.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-15"), hasShares(2.140), //
                        hasSource("Quartalsbericht01.txt"), hasNote(null), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2021-05-28T00:00"), hasShares(1.315), //
                                        hasSource("Quartalsbericht01.txt"), //
                                        hasNote("Fusion"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2021-05-28T00:00"), hasShares(2.140), //
                                        hasSource("Quartalsbericht01.txt"), //
                                        hasNote("Fusion"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testQuartalsbericht02()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht02.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(9L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2CDS0"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-DividendenStrategie CF (A)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1508359509"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Industrie 4.0 CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474503"), hasWkn(null), hasTicker(null), //
                        hasName("DekaFonds CF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-11"), hasShares(0.255), //
                        hasSource("Quartalsbericht02.txt"), hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-10"), hasShares(0.268), //
                        hasSource("Quartalsbericht02.txt"), hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2022-05-20T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 1st security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-10"), hasShares(0.271), //
                        hasSource("Quartalsbericht02.txt"), hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-11"), hasShares(0.545), //
                        hasSource("Quartalsbericht02.txt"), hasNote(null), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-10"), hasShares(0.616), //
                        hasSource("Quartalsbericht02.txt"), hasNote(null), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-10"), hasShares(0.598), //
                        hasSource("Quartalsbericht02.txt"), hasNote(null), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-11"), hasShares(0.418), //
                        hasSource("Quartalsbericht02.txt"), hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-10"), hasShares(0.443), //
                        hasSource("Quartalsbericht02.txt"), hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-10"), hasShares(0.430), //
                        hasSource("Quartalsbericht02.txt"), hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2022-05-20T00:00"), hasExDate("2022-05-20T00:00"), //
                                        hasShares(15.396 + 0.255 + 0.268), //
                                        hasSource("Quartalsbericht02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 1nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-20"), hasExDate("2022-05-20"), //
                        hasShares(16.190 - 0.271 - 0.000), //
                        hasSource("Quartalsbericht02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.43), hasGrossValue("EUR", 33.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht03()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht03.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("DE000DK2CFT3"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BasisAnlage offensiv"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-18"), hasShares(0.167), //
                        hasSource("Quartalsbericht03.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-08-16"), hasShares(0.155), //
                        hasSource("Quartalsbericht03.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-09-16"), hasShares(0.162), //
                        hasSource("Quartalsbericht03.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht04()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht04.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK0ECU8"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GlobalChampions CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-21"), hasShares(0.140), //
                        hasSource("Quartalsbericht04.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-23"), hasShares(0.151), //
                        hasSource("Quartalsbericht04.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-21"), hasShares(0.156), //
                        hasSource("Quartalsbericht04.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-08"), hasShares(0.775), //
                        hasSource("Quartalsbericht04.txt"), hasNote(null), //
                        hasAmount("EUR", 80.00), hasGrossValue("EUR", 80.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2022-05-17"), hasShares(0.147), //
                        hasSource("Quartalsbericht04.txt"), hasNote("Zulagenzahlung 2021"), //
                        hasAmount("EUR", 14.99), hasGrossValue("EUR", 14.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht05()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht05.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(10L));
        assertThat(countBuySell(results), is(25L));
        assertThat(countAccountTransactions(results), is(11L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(46));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK0ECU8"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GlobalChampions CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005152631"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Technologie TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLuxTeam-Aktien Asien"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062625115"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Europa TF (A)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771980"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaBond TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-01-10"), hasShares(0.228), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-05"), hasShares(0.216), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-15"), hasShares(0.008), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 1.46), hasGrossValue("EUR", 1.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-11"), hasShares(0.212), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-10"), hasShares(0.202), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-05-09"), hasShares(0.203), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-06-11"), hasShares(0.204), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-07-11"), hasShares(0.196), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-12"), hasShares(0.202), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-09-11"), hasShares(0.196), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-10-11"), hasShares(0.196), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-11"), hasShares(0.186), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-10"), hasShares(0.184), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-16"), hasShares(0.194), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 54.52), hasGrossValue("EUR", 54.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-22"), hasShares(0.170), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 5.08), hasGrossValue("EUR", 5.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-15"), hasShares(0.121), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 20.88), hasGrossValue("EUR", 20.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-15"), hasShares(0.020), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 14.44), hasGrossValue("EUR", 14.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 18th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-15"), hasShares(0.034), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 4.44), hasGrossValue("EUR", 4.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 19th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-15"), hasShares(0.045), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 2.83), hasGrossValue("EUR", 2.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 20th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-16"), hasShares(0.140), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 6.14), hasGrossValue("EUR", 6.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 21th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-22"), hasShares(0.098), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 11.52), hasGrossValue("EUR", 11.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 22th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-06-11"), hasShares(1.00), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 80.00), hasGrossValue("EUR", 80.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 23th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-06-25"), hasShares(0.015), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 1.17), hasGrossValue("EUR", 1.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 24th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-10-21"), hasShares(13.373), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 1120.00), hasGrossValue("EUR", 1120.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 25th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-10"), hasShares(0.019), //
                        hasSource("Quartalsbericht05.txt"), hasNote(null), //
                        hasAmount("EUR", 1.61), hasGrossValue("EUR", 1.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2019-05-16"), hasShares(2.197), //
                        hasSource("Quartalsbericht05.txt"), hasNote("Zulagenzahlung 2018"), //
                        hasAmount("EUR", 175.00), hasGrossValue("EUR", 175.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-02-15"), hasExDate("2019-02-15"), //
                        hasShares(1.981 + 0.228 + 0.216), //
                        hasSource("Quartalsbericht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.46), hasGrossValue("EUR", 1.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-08-16"), hasExDate("2019-08-16"), //
                        hasShares(13.461), //
                        hasSource("Quartalsbericht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 54.52), hasGrossValue("EUR", 54.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-02-22"), hasExDate("2019-02-22"), //
                        hasShares(50.822), //
                        hasSource("Quartalsbericht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.08), hasGrossValue("EUR", 5.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-15"), hasExDate("2019-11-15"), //
                        hasShares(5.598), //
                        hasSource("Quartalsbericht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 20.88), hasGrossValue("EUR", 20.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-15"), hasExDate("2019-11-15"), //
                        hasShares(6.391), //
                        hasSource("Quartalsbericht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.44), hasGrossValue("EUR", 14.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-15"), hasExDate("2019-11-15"), //
                        hasShares(11.394), //
                        hasSource("Quartalsbericht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.44), hasGrossValue("EUR", 4.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-15"), hasExDate("2019-11-15"), //
                        hasShares(6.892), //
                        hasSource("Quartalsbericht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.83), hasGrossValue("EUR", 2.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-08-16"), hasExDate("2019-08-16"), //
                        hasShares(15.756), //
                        hasSource("Quartalsbericht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 6.14), hasGrossValue("EUR", 6.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-02-22"), hasExDate("2019-02-22"), //
                        hasShares(29.546), //
                        hasSource("Quartalsbericht05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 11.52), hasGrossValue("EUR", 11.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2019-12-31"), hasAmount("EUR", 12.50), //
                        hasSource("Quartalsbericht05.txt"), hasNote("Depotpreis 2019"))));
    }

    @Test
    public void testQuartalsbericht06()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht06.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(9L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(11L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(6L));
        assertThat(countSkippedItems(results), is(6L));
        assertThat(results.size(), is(30));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005152631"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Technologie TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062625115"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Europa TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771980"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaBond TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2012-07-02T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2012-12-28T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2012-10-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 1th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-11-20"), hasShares(0.125), //
                        hasSource("Quartalsbericht06.txt"), hasNote(null), //
                        hasAmount("EUR", 54.72), hasGrossValue("EUR", 54.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2012-10-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2012-10-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 2th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-08-20"), hasShares(0.373), //
                        hasSource("Quartalsbericht06.txt"), hasNote(null), //
                        hasAmount("EUR", 15.29), hasGrossValue("EUR", 15.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2012-12-28T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 3th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-06-08"), hasShares(2.006), //
                        hasSource("Quartalsbericht06.txt"), hasNote(null), //
                        hasAmount("EUR", 80.00), hasGrossValue("EUR", 80.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-07-02"), hasShares(0.110), //
                        hasSource("Quartalsbericht06.txt"), hasNote(null), //
                        hasAmount("EUR", 4.39), hasGrossValue("EUR", 4.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2012-05-21"), hasShares(2.621), //
                        hasSource("Quartalsbericht06.txt"), hasNote("Zulagenzahlung 2011"), //
                        hasAmount("EUR", 101.91), hasGrossValue("EUR", 101.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2012-08-20"), hasExDate("2012-08-20"), //
                        hasShares(13.772), //
                        hasSource("Quartalsbericht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 15.29), hasGrossValue("EUR", 15.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2012-11-20"), hasExDate("2012-11-20"), //
                        hasShares(5.791), //
                        hasSource("Quartalsbericht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 54.72), hasGrossValue("EUR", 54.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2012-07-02T00:00"), hasExDate("2012-07-02T00:00"), //
                                        hasShares(12.778), //
                                        hasSource("Quartalsbericht06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2012-12-28T00:00"), hasExDate("2012-12-28T00:00"), //
                                        hasShares(50.169), //
                                        hasSource("Quartalsbericht06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2012-10-01T00:00"), hasExDate("2012-10-01T00:00"), //
                                        hasShares(5.454), //
                                        hasSource("Quartalsbericht06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2012-10-01T00:00"), hasExDate("2012-10-01T00:00"), //
                                        hasShares(10.694), //
                                        hasSource("Quartalsbericht06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2012-10-01T00:00"), hasExDate("2012-10-01T00:00"), //
                                        hasShares(6.332), //
                                        hasSource("Quartalsbericht06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2012-12-28T00:00"), hasExDate("2012-12-28T00:00"), //
                                        hasShares(28.237), //
                                        hasSource("Quartalsbericht06.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 9th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2012-07-02"), hasExDate("2012-07-02"), //
                        hasShares(33.898 - 0.110), //
                        hasSource("Quartalsbericht06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.39), hasGrossValue("EUR", 4.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2012-12-31T00:00"), //
                        hasSource("Quartalsbericht06.txt"), //
                        hasNote("Depotpreis 2012"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht07()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht07.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(14));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2CFT3"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BasisAnlage offensiv"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-18"), hasShares(0.149), //
                        hasSource("Quartalsbericht07.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-02-16"), hasShares(0.155), //
                        hasSource("Quartalsbericht07.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-16"), hasShares(0.165), //
                        hasSource("Quartalsbericht07.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-20"), hasShares(0.158), //
                        hasSource("Quartalsbericht07.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-05-17"), hasShares(0.165), //
                        hasSource("Quartalsbericht07.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-17"), hasShares(0.174), //
                        hasSource("Quartalsbericht07.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-18"), hasShares(0.167), //
                        hasSource("Quartalsbericht07.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-08-16"), hasShares(0.155), //
                        hasSource("Quartalsbericht07.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-09-16"), hasShares(0.162), //
                        hasSource("Quartalsbericht07.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-10-18"), hasShares(0.172), //
                        hasSource("Quartalsbericht07.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-16"), hasShares(0.163), //
                        hasSource("Quartalsbericht07.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-12-16"), hasShares(0.165), //
                        hasSource("Quartalsbericht07.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2022-12-31"), hasAmount("EUR", 12.50), //
                        hasSource("Quartalsbericht07.txt"), hasNote("Depotpreis 2022"))));
    }

    @Test
    public void testQuartalsbericht08()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht08.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK0ECU8"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GlobalChampions CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-23"), hasShares(0.153), //
                        hasSource("Quartalsbericht08.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-10"), hasShares(0.176), //
                        hasSource("Quartalsbericht08.txt"), hasNote(null), //
                        hasAmount("EUR", 45.04), hasGrossValue("EUR", 45.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-21"), hasShares(0.151), //
                        hasSource("Quartalsbericht08.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-03-21"), hasShares(0.154), //
                        hasSource("Quartalsbericht08.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-24"), hasShares(0.498), //
                        hasSource("Quartalsbericht08.txt"), hasNote(null), //
                        hasAmount("EUR", 69.10), hasGrossValue("EUR", 69.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2023-01-06"), hasShares(0.016), //
                        hasSource("Quartalsbericht08.txt"), hasNote("Steuererstattung"), //
                        hasAmount("EUR", 1.57), hasGrossValue("EUR", 1.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-02-10"), hasExDate("2023-02-10"), //
                        hasShares(10.297 + 0.153), //
                        hasSource("Quartalsbericht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 45.04), hasGrossValue("EUR", 45.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-02-24"), hasExDate("2023-02-24"), //
                        hasShares(29.658), //
                        hasSource("Quartalsbericht08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 69.10), hasGrossValue("EUR", 69.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht09()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht09.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(10L));
        assertThat(countBuySell(results), is(34L));
        assertThat(countAccountTransactions(results), is(16L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(5L));
        assertThat(countSkippedItems(results), is(4L));
        assertThat(results.size(), is(64));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771824"), hasWkn(null), hasTicker(null), //
                        hasName("Euro TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771980"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaBond TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0002262826"), hasWkn(null), hasTicker(null), //
                        hasName("DekaTeam-GlobalSelect TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0004907972"), hasWkn(null), hasTicker(null), //
                        hasName("DekaTeam-EmergingMarkets CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0005258151"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0097654924"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuroStocks TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-01-31"), hasShares(0.630), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-02-18"), hasShares(0.032), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 1.69), hasGrossValue("EUR", 1.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-02-28"), hasShares(0.615), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-03-30"), hasShares(0.622), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-04-28"), hasShares(0.636), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-05-30"), hasShares(0.608), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-06-29"), hasShares(0.588), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-07-29"), hasShares(0.564), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-08-30"), hasShares(0.573), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-09-29"), hasShares(0.546), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-10-28"), hasShares(0.574), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-11-30"), hasShares(0.539), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-11-21"), hasShares(0.161), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 58.11), hasGrossValue("EUR", 58.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-11-22"), hasShares(1.166), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 440.00), hasGrossValue("EUR", 440.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-06-28"), hasShares(16.858), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2005-08-09"), hasShares(25), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 1594.00), hasGrossValue("EUR", 1594.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-11-21"), hasShares(0.054), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 3.64), hasGrossValue("EUR", 3.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 18th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2005-12-16"), hasShares(0.143), //
                        hasSource("Quartalsbericht09.txt"), hasNote("Depotpreis 2005"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 19th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-01-03"), hasShares(0.004), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 0.28), hasGrossValue("EUR", 0.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2005-12-30T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht09.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 21th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-01-03"), hasShares(10), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 400.20), hasGrossValue("EUR", 400.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 22th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-02-01"), hasShares(7), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 283.36), hasGrossValue("EUR", 283.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 23th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-03-02"), hasShares(8), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 327.12), hasGrossValue("EUR", 327.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 24th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-04-01"), hasShares(8), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 324.88), hasGrossValue("EUR", 324.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 25th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-08-22"), hasShares(2.494), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 99.63), hasGrossValue("EUR", 99.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 26th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-01-03"), hasShares(0.002), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 27th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-10-04"), hasShares(5.854), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 400.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 28th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-11-21"), hasShares(8.782), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 560.00), hasGrossValue("EUR", 560.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2005-12-30T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht09.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 30th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-08-22"), hasShares(0.100), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 3.70), hasGrossValue("EUR", 3.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 31th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2005-11-21"), hasShares(23.223), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 911.50), hasGrossValue("EUR", 911.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 32th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-08-22"), hasShares(0.451), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 21.94), hasGrossValue("EUR", 21.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 33th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-08-22"), hasShares(0.025), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 1.05), hasGrossValue("EUR", 1.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2005-09-09T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht09.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 35th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-08-22"), hasShares(0.025), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 1.05), hasGrossValue("EUR", 1.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2005-09-14T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht09.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 37th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2005-11-22"), hasShares(12.949), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 570.00), hasGrossValue("EUR", 570.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 38th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2005-11-18"), hasShares(18.812), //
                        hasSource("Quartalsbericht09.txt"), hasNote(null), //
                        hasAmount("EUR", 641.11), hasGrossValue("EUR", 641.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2005-02-18"), hasExDate("2005-02-18"), //
                        hasShares(9.135 - 0.539 - 0.574 - 0.546 - 0.573 - 0.564 - 0.588 - 0.608 - 0.636 - 0.622 - 0.615 - 0.032), //
                        hasSource("Quartalsbericht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.69), hasGrossValue("EUR", 1.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2005-11-21"), hasExDate("2005-11-21"), //
                        hasShares(5.475 - 1.166 - 0.161), //
                        hasSource("Quartalsbericht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 58.11), hasGrossValue("EUR", 58.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2005-11-21"), hasExDate("2005-11-21"), //
                        hasShares(20.130 + 0.143 - 0.054), //
                        hasSource("Quartalsbericht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.64), hasGrossValue("EUR", 3.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2005-01-03"), hasExDate("2005-01-03"), //
                        hasShares(0.909), //
                        hasSource("Quartalsbericht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.28), hasGrossValue("EUR", 0.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2005-08-22"), hasExDate("2005-08-22"), //
                        hasShares(43.494 - 2.494), //
                        hasSource("Quartalsbericht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 99.63), hasGrossValue("EUR", 99.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2005-01-03"), hasExDate("2005-01-03"), //
                        hasShares(45.986 - 0.000 - 8.782 - 5.854 - 0.002), //
                        hasSource("Quartalsbericht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2005-08-22"), hasExDate("2005-08-22"), //
                        hasShares(0.000 + 23.223 - 0.100), //
                        hasSource("Quartalsbericht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.70), hasGrossValue("EUR", 3.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2005-08-22"), hasExDate("2005-08-22"), //
                        hasShares(26.268 - 0.451), //
                        hasSource("Quartalsbericht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 21.94), hasGrossValue("EUR", 21.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2005-12-30T00:00"), hasExDate("2005-12-30T00:00"), //
                                        hasShares(0.913), //
                                        hasSource("Quartalsbericht09.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2005-12-30T00:00"), hasExDate("2005-12-30T00:00"), //
                                        hasShares(31.348 + 0.002 + 5.854 + 8.782), //
                                        hasSource("Quartalsbericht09.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2005-09-09T00:00"), hasExDate("2005-09-09T00:00"), //
                                        hasShares(26.194), //
                                        hasSource("Quartalsbericht09.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2005-09-14T00:00"), hasExDate("2005-09-14T00:00"), //
                                        hasShares(26.194 + 0.025), //
                                        hasSource("Quartalsbericht09.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2005-12-16T00:00"), hasShares(0.00), //
                        hasSource("Quartalsbericht09.txt"), hasNote("Depotpreis 2005"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2005-08-22"), hasExDate("2005-08-22"), //
                        hasShares(26.194), //
                        hasSource("Quartalsbericht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.05), hasGrossValue("EUR", 1.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2005-09-07"), hasExDate("2005-08-22"), //
                                        hasShares(0.00), //
                                        hasSource("Quartalsbericht09.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 1.05), hasGrossValue("EUR", 1.05), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2005-09-09"), hasExDate("2005-08-22"), //
                        hasShares(0.00), //
                        hasSource("Quartalsbericht09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.05), hasGrossValue("EUR", 1.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht10()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht10.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(9L));
        assertThat(countBuySell(results), is(28L));
        assertThat(countAccountTransactions(results), is(11L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(2L));
        assertThat(results.size(), is(50));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771824"), hasWkn(null), hasTicker(null), //
                        hasName("Euro TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771980"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaBond TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0004907972"), hasWkn(null), hasTicker(null), //
                        hasName("DekaTeam-EmergingMarkets CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0005258151"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-12-11"), hasShares(1.198), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 60.00), hasGrossValue("EUR", 60.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2006-12-15"), hasShares(0.102), //
                        hasSource("Quartalsbericht10.txt"), hasNote("Vertragsgebühr 2006"), //
                        hasAmount("EUR", 5.00), hasGrossValue("EUR", 5.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-12-28"), hasShares(2.514), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 128.25), hasGrossValue("EUR", 128.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-01-02"), hasShares(0.523), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-01-31"), hasShares(0.505), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-02-17"), hasShares(0.129), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 8.22), hasGrossValue("EUR", 8.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-02-28"), hasShares(0.499), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-03-31"), hasShares(0.496), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-04-28"), hasShares(0.493), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-06-01"), hasShares(0.521), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-07-03"), hasShares(0.516), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-08-01"), hasShares(0.508), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-08-31"), hasShares(0.496), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-09-29"), hasShares(0.489), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-10-31"), hasShares(0.471), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-11-30"), hasShares(0.475), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-12-28"), hasShares(0.460), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 18th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-11-20"), hasShares(0.157), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 65.65), hasGrossValue("EUR", 65.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 19th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-11-07"), hasShares(12.152), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 20th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-11-20"), hasShares(0.004), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 0.32), hasGrossValue("EUR", 0.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 21th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2006-12-15"), hasShares(0.117), //
                        hasSource("Quartalsbericht10.txt"), hasNote("Depotpreis 2006"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 22th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-01-02"), hasShares(0.005), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 0.31), hasGrossValue("EUR", 0.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 23th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-10-09"), hasShares(15.323), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2006-12-29T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht10.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 25th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-08-21"), hasShares(1.771), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 66.98), hasGrossValue("EUR", 66.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 26th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-01-03"), hasShares(8.699), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 600.00), hasGrossValue("EUR", 600.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2006-12-29T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht10.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 28th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-08-21"), hasShares(0.461), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 26.79), hasGrossValue("EUR", 26.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 29th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-01-04"), hasShares(9.089), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 400.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 30th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2006-08-21"), hasShares(0.070), //
                        hasSource("Quartalsbericht10.txt"), hasNote(null), //
                        hasAmount("EUR", 2.90), hasGrossValue("EUR", 2.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2006-02-17"), hasExDate("2006-02-17"), //
                        hasShares(15.716 - 0.460 - 0.475 - 0.471 - 0.489 - 0.496 - 0.508 - 0.516 - 0.521 - 0.493 - 0.496 - 0.499 - 0.129), //
                        hasSource("Quartalsbericht10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.22), hasGrossValue("EUR", 8.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2006-11-20"), hasExDate("2006-11-20"), //
                        hasShares(5.475), //
                        hasSource("Quartalsbericht10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 65.65), hasGrossValue("EUR", 65.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2006-11-20"), hasExDate("2006-11-20"), //
                        hasShares(20.130 + 12.152), //
                        hasSource("Quartalsbericht10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.32), hasGrossValue("EUR", 0.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2006-01-02"), hasExDate("2006-01-02"), //
                        hasShares(0.913), //
                        hasSource("Quartalsbericht10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.31), hasGrossValue("EUR", 0.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2006-12-29T00:00"), hasExDate("2006-12-29T00:00"), //
                                        hasShares(0.913 + 0.005 + 15.323), //
                                        hasSource("Quartalsbericht10.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 6th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2006-08-21"), hasExDate("2006-08-21"), //
                        hasShares(43.494), //
                        hasSource("Quartalsbericht10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 66.98), hasGrossValue("EUR", 66.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2006-12-29T00:00"), hasExDate("2006-12-29T00:00"), //
                                        hasShares(45.986 + 8.699), //
                                        hasSource("Quartalsbericht10.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 8th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2006-08-21"), hasExDate("2006-08-21"), //
                        hasShares(26.268), //
                        hasSource("Quartalsbericht10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.79), hasGrossValue("EUR", 26.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2006-08-21"), hasExDate("2006-08-21"), //
                        hasShares(39.168 + 9.089), //
                        hasSource("Quartalsbericht10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.90), hasGrossValue("EUR", 2.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2006-12-15T00:00"), hasShares(0), //
                        hasSource("Quartalsbericht10.txt"), hasNote("Depotpreis 2006"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2006-12-15T00:00"), hasShares(0), //
                        hasSource("Quartalsbericht10.txt"), hasNote("Vertragsgebühr 2006"), //
                        hasAmount("EUR", 5.00), hasGrossValue("EUR", 5.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht11()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht11.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(10L));
        assertThat(countBuySell(results), is(32L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(2L));
        assertThat(results.size(), is(54));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0268059614"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GeldmarktPlan TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771824"), hasWkn(null), hasTicker(null), //
                        hasName("Euro TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771980"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaBond TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE0005258151"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-09-03"), hasShares(0.598), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 600.00), hasGrossValue("EUR", 600.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2008-12-12"), hasShares(0.010), //
                        hasSource("Quartalsbericht11.txt"), hasNote("Depotpreis 2008"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-06-30"), hasShares(15.795), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 1502.30), hasGrossValue("EUR", 1502.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2007-12-27"), hasShares(3.952), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 188.25), hasGrossValue("EUR", 188.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-07-01"), hasShares(0.007), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 0.27), hasGrossValue("EUR", 0.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-08-18"), hasShares(2.926), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 114.00), hasGrossValue("EUR", 114.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-10-01"), hasShares(7.494), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 270.00), hasGrossValue("EUR", 270.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2008-12-12"), hasShares(0.357), //
                        hasSource("Quartalsbericht11.txt"), hasNote("Vertragsgebühr 2008"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-01-31"), hasShares(0.519), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-02-22"), hasShares(0.258), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 16.09), hasGrossValue("EUR", 16.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-02-29"), hasShares(0.519), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-03-31"), hasShares(0.551), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-04-30"), hasShares(0.519), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-05-30"), hasShares(0.508), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-06-30"), hasShares(0.578), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-07-31"), hasShares(0.577), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-08-29"), hasShares(0.571), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 18th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-09-30"), hasShares(0.651), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 19th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-10-31"), hasShares(0.775), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 20th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-11-28"), hasShares(0.817), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 21th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-12-30"), hasShares(0.848), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 22th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-03-07"), hasShares(5.761), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 997.36), hasGrossValue("EUR", 997.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 23th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-04-04"), hasShares(3.531), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 600.00), hasGrossValue("EUR", 600.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 24th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-06-13"), hasShares(2.473), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 450.00), hasGrossValue("EUR", 450.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 25th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-07-01"), hasShares(3.451), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 600.00), hasGrossValue("EUR", 600.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2008-10-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht11.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 27th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-11-20"), hasShares(0.161), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 38.51), hasGrossValue("EUR", 38.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 28th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-05-05"), hasShares(2.270), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 200.00), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 29th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-11-20"), hasShares(0.078), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 3.55), hasGrossValue("EUR", 3.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 30th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-01-02"), hasShares(0.168), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 11.13), hasGrossValue("EUR", 11.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 31th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2008-03-07"), hasShares(15.025), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 997.36), hasGrossValue("EUR", 997.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 32th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-08-20"), hasShares(1.998), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 72.52), hasGrossValue("EUR", 72.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2008-06-30T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht11.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 34th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2008-06-30"), hasShares(48.399), //
                        hasSource("Quartalsbericht11.txt"), hasNote(null), //
                        hasAmount("EUR", 1502.30), hasGrossValue("EUR", 1502.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2008-07-01"), hasExDate("2008-07-01"), //
                        hasShares(5.529 + 3.952), //
                        hasSource("Quartalsbericht11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.27), hasGrossValue("EUR", 0.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2008-02-22"), hasExDate("2008-02-22"), //
                        hasShares(21.253 + 0.519), //
                        hasSource("Quartalsbericht11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.09), hasGrossValue("EUR", 16.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2008-10-01T00:00"), hasExDate("2008-10-01T00:00"), //
                                        hasShares(15.216), //
                                        hasSource("Quartalsbericht11.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2008-11-20"), hasExDate("2008-11-20"), //
                        hasShares(5.934 - 0.161), //
                        hasSource("Quartalsbericht11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 38.51), hasGrossValue("EUR", 38.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2008-11-20"), hasExDate("2008-11-20"), //
                        hasShares(32.324 - 0.078), //
                        hasSource("Quartalsbericht11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.55), hasGrossValue("EUR", 3.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2008-01-02"), hasExDate("2008-01-02"), //
                        hasShares(0.00 + 15.025 - 0.168), //
                        hasSource("Quartalsbericht11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 11.13), hasGrossValue("EUR", 11.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2008-08-20"), hasExDate("2008-08-20"), //
                        hasShares(49.088 - 1.998), //
                        hasSource("Quartalsbericht11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 72.52), hasGrossValue("EUR", 72.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2008-06-30T00:00"), hasExDate("2008-06-30T00:00"), //
                                        hasShares(0.000 + 48.399 - 0.000), //
                                        hasSource("Quartalsbericht11.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2008-12-12T00:00"), hasShares(0), //
                        hasSource("Quartalsbericht11.txt"), hasNote("Depotpreis 2008"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2008-12-12T00:00"), hasShares(0), //
                        hasSource("Quartalsbericht11.txt"), hasNote("Vertragsgebühr 2008"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht12()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht12.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(9L));
        assertThat(countBuySell(results), is(14L));
        assertThat(countAccountTransactions(results), is(14L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(3L));
        assertThat(countSkippedItems(results), is(3L));
        assertThat(results.size(), is(40));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0268059614"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GeldmarktPlan TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771980"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaBond TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-06-16"), hasShares(0.977), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-08-20"), hasShares(0.047), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 46.31), hasGrossValue("EUR", 46.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-10-02"), hasShares(0.402), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 400.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-03-06"), hasShares(1.373), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 1400.00), hasGrossValue("EUR", 1400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2009-03-27"), hasShares(1.373), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 1401.16), hasGrossValue("EUR", 1401.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2009-07-02T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht12.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-07-01"), hasShares(0.065), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 1.83), hasGrossValue("EUR", 1.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-10-01"), hasShares(8.326), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 270.00), hasGrossValue("EUR", 270.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-01-30"), hasShares(0.867), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-02-20"), hasShares(0.872), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 29.15), hasGrossValue("EUR", 29.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-02-27"), hasShares(0.998), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 34.00), hasGrossValue("EUR", 34.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2009-10-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht12.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-11-20"), hasShares(0.098), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 38.27), hasGrossValue("EUR", 38.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-11-20"), hasShares(0.047), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 3.23), hasGrossValue("EUR", 3.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-08-20"), hasShares(1.418), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 51.54), hasGrossValue("EUR", 51.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2009-02-20"), hasShares(0.095), //
                        hasSource("Quartalsbericht12.txt"), hasNote(null), //
                        hasAmount("EUR", 3.60), hasGrossValue("EUR", 3.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2009-12-30T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht12.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2009-11-17"), hasShares(10.744), //
                        hasSource("Quartalsbericht12.txt"), hasNote("Kauf Zulagenzahlung"), //
                        hasAmount("EUR", 354.00), hasGrossValue("EUR", 354.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-08-20"), hasExDate("2009-08-20"), //
                        hasShares(2.014 - 0.402 - 0.047), //
                        hasSource("Quartalsbericht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 46.31), hasGrossValue("EUR", 46.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2009-07-02T00:00"), hasExDate("2009-07-02T00:00"), //
                                        hasShares(15.795), //
                                        hasSource("Quartalsbericht12.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-07-01"), hasExDate("2009-07-01"), //
                        hasShares(38.686 - 10.744 - 8.326 - 0.065), //
                        hasSource("Quartalsbericht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.83), hasGrossValue("EUR", 1.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-02-20"), hasExDate("2009-02-20"), //
                        hasShares(31.681 - 0.998 - 0.872), //
                        hasSource("Quartalsbericht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.15), hasGrossValue("EUR", 29.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2009-10-01T00:00"), hasExDate("2009-10-01T00:00"), //
                                        hasShares(15.216), //
                                        hasSource("Quartalsbericht12.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 6th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-11-20"), hasExDate("2009-11-20"), //
                        hasShares(6.032 - 0.098), //
                        hasSource("Quartalsbericht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 38.27), hasGrossValue("EUR", 38.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-11-20"), hasExDate("2009-11-20"), //
                        hasShares(32.371 - 0.047), //
                        hasSource("Quartalsbericht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.23), hasGrossValue("EUR", 3.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2009-12-31T00:00"), hasShares(0), //
                        hasSource("Quartalsbericht12.txt"), hasNote("Depotpreis 2009"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2009-12-31T00:00"), hasShares(0), //
                        hasSource("Quartalsbericht12.txt"), hasNote("Abschluss-/ Vertriebskosten 2009"), //
                        hasAmount("EUR", 21.06), hasGrossValue("EUR", 21.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2009-12-31T00:00"), hasShares(0), //
                        hasSource("Quartalsbericht12.txt"), hasNote("Vertragspreis 2009"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-02-20"), hasExDate("2009-02-20"), //
                        hasShares(35.95), //
                        hasSource("Quartalsbericht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.60), hasGrossValue("EUR", 3.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2009-08-20"), hasExDate("2009-08-20"), //
                        hasShares(49.088), //
                        hasSource("Quartalsbericht12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 51.54), hasGrossValue("EUR", 51.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2009-12-30"), hasExDate("2009-12-30"), //
                                        hasShares(36.045), //
                                        hasSource("Quartalsbericht12.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testQuartalsbericht13()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht13.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(9L));
        assertThat(countBuySell(results), is(10L));
        assertThat(countAccountTransactions(results), is(14L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(5L));
        assertThat(countSkippedItems(results), is(8L));
        assertThat(results.size(), is(41));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0268059614"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-GeldmarktPlan TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771980"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaBond TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2010-04-16"), hasShares(1.001), //
                        hasSource("Quartalsbericht13.txt"), hasNote(null), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-08-20"), hasShares(0.010), //
                        hasSource("Quartalsbericht13.txt"), hasNote(null), //
                        hasAmount("EUR", 9.93), hasGrossValue("EUR", 9.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2010-07-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-02-19"), hasShares(0.331), //
                        hasSource("Quartalsbericht13.txt"), hasNote(null), //
                        hasAmount("EUR", 15.06), hasGrossValue("EUR", 15.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-12-14"), hasShares(1.339), //
                        hasSource("Quartalsbericht13.txt"), hasNote(null), //
                        hasAmount("EUR", 72.00), hasGrossValue("EUR", 72.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-12-14"), hasShares(0.465), //
                        hasSource("Quartalsbericht13.txt"), hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2010-10-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-04-16"), hasShares(5.864), //
                        hasSource("Quartalsbericht13.txt"), hasNote(null), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2010-10-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-11-22"), hasShares(0.058), //
                        hasSource("Quartalsbericht13.txt"), hasNote(null), //
                        hasAmount("EUR", 27.51), hasGrossValue("EUR", 27.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2010-10-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-08-20"), hasShares(1.696), //
                        hasSource("Quartalsbericht13.txt"), hasNote(null), //
                        hasAmount("EUR", 66.16), hasGrossValue("EUR", 66.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2010-12-30T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-07-01"), hasShares(0.059), //
                        hasSource("Quartalsbericht13.txt"), hasNote(null), //
                        hasAmount("EUR", 2.02), hasGrossValue("EUR", 2.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-10-01"), hasShares(7.448), //
                        hasSource("Quartalsbericht13.txt"), hasNote(null), //
                        hasAmount("EUR", 270.00), hasGrossValue("EUR", 270.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2010-05-19"), hasShares(1.246), //
                        hasSource("Quartalsbericht13.txt"), hasNote("Zulagenzahlung 2009"), //
                        hasAmount("EUR", 47.18), hasGrossValue("EUR", 47.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-08-20"), hasExDate("2010-08-20"), //
                        hasShares(1.023 - 0.010), //
                        hasSource("Quartalsbericht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.93), hasGrossValue("EUR", 9.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2010-07-01T00:00"), hasExDate("2010-07-01T00:00"), //
                                        hasShares(15.795), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-02-19"), hasExDate("2010-02-19"), //
                        hasShares(33.816 - 0.465 - 1.339 - 0.331), //
                        hasSource("Quartalsbericht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 15.06), hasGrossValue("EUR", 15.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2010-10-01T00:00"), hasExDate("2010-10-01T00:00"), //
                                        hasShares(15.216), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2010-10-01T00:00"), hasExDate("2010-10-01T00:00"), //
                                        hasShares(5.864), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 6th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-11-22"), hasExDate("2010-11-22"), //
                        hasShares(6.090 - 0.058), //
                        hasSource("Quartalsbericht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 27.51), hasGrossValue("EUR", 27.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2010-10-01T00:00"), hasExDate("2010-10-01T00:00"), //
                                        hasShares(32.371), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 8th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-08-20"), hasExDate("2010-08-20"), //
                        hasShares(52.202 - 1.696), //
                        hasSource("Quartalsbericht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 66.16), hasGrossValue("EUR", 66.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2010-12-30T00:00"), hasExDate("2010-12-30T00:00"), //
                                        hasShares(36.045), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 10th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-07-01"), hasExDate("2010-07-01"), //
                        hasShares(47.439 - 7.448 - 0.059), //
                        hasSource("Quartalsbericht13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.02), hasGrossValue("EUR", 2.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        fee( //
                                        hasDate("2010-12-31T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote("Vertragspreis (zu Lasten Vertrag) 2010"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        fee( //
                                        hasDate("2010-12-31T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote("Weitere Preise (zu Lasten Girokonto) 2010"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        fee( //
                                        hasDate("2010-12-31T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht13.txt"), //
                                        hasNote("Weitere Preise (zu Lasten Vertrag) 2010"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2010-12-31T00:00"), hasShares(0), //
                        hasSource("Quartalsbericht13.txt"), hasNote("Depotpreis 2010"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2010-12-31T00:00"), hasShares(0), //
                        hasSource("Quartalsbericht13.txt"), hasNote("Abschluss-/ Vertriebskosten 2010"), //
                        hasAmount("EUR", 10.77), hasGrossValue("EUR", 10.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2010-12-31T00:00"), hasShares(0), //
                        hasSource("Quartalsbericht13.txt"), hasNote("Vertragspreis (zu Lasten Girokonto) 2010"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht14()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht14.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(9L));
        assertThat(countBuySell(results), is(7L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(4L));
        assertThat(countSkippedItems(results), is(4L));
        assertThat(results.size(), is(32));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0268059614"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-LiquiditätsPlan TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771980"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaBond TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-08-19"), hasShares(0.013), //
                        hasSource("Quartalsbericht14.txt"), hasNote(null), //
                        hasAmount("EUR", 12.48), hasGrossValue("EUR", 12.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2011-07-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht14.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-02-18"), hasShares(0.166), //
                        hasSource("Quartalsbericht14.txt"), hasNote(null), //
                        hasAmount("EUR", 8.88), hasGrossValue("EUR", 8.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2011-10-04T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht14.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2011-10-04T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht14.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-11-21"), hasShares(0.100), //
                        hasSource("Quartalsbericht14.txt"), hasNote(null), //
                        hasAmount("EUR", 38.73), hasGrossValue("EUR", 38.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-11-21"), hasShares(0.074), //
                        hasSource("Quartalsbericht14.txt"), hasNote(null), //
                        hasAmount("EUR", 4.86), hasGrossValue("EUR", 4.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-08-19"), hasShares(1.580), //
                        hasSource("Quartalsbericht14.txt"), hasNote(null), //
                        hasAmount("EUR", 60.03), hasGrossValue("EUR", 60.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2011-12-30T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht14.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-07-01"), hasShares(0.051), //
                        hasSource("Quartalsbericht14.txt"), hasNote(null), //
                        hasAmount("EUR", 1.92), hasGrossValue("EUR", 1.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2011-10-04"), hasShares(8.077), //
                        hasSource("Quartalsbericht14.txt"), hasNote(null), //
                        hasAmount("EUR", 270.00), hasGrossValue("EUR", 270.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2011-05-19"), hasShares(2.739), //
                        hasSource("Quartalsbericht14.txt"), hasNote("Zulagenzahlung 2010"), //
                        hasAmount("EUR", 108.89), hasGrossValue("EUR", 108.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-08-19"), hasExDate("2011-08-19"), //
                        hasShares(1.036 - 0.013), //
                        hasSource("Quartalsbericht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.48), hasGrossValue("EUR", 12.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2011-07-01T00:00"), hasExDate("2011-07-01T00:00"), //
                                        hasShares(15.795), //
                                        hasSource("Quartalsbericht14.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-02-18"), hasExDate("2011-02-18"), //
                        hasShares(33.982 - 0.166), //
                        hasSource("Quartalsbericht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.88), hasGrossValue("EUR", 8.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2011-10-04T00:00"), hasExDate("2011-10-04T00:00"), //
                                        hasShares(15.216), //
                                        hasSource("Quartalsbericht14.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2011-10-04T00:00"), hasExDate("2011-10-04T00:00"), //
                                        hasShares(5.864), //
                                        hasSource("Quartalsbericht14.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 6th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-11-21"), hasExDate("2011-11-21"), //
                        hasShares(6.190 - 0.100), //
                        hasSource("Quartalsbericht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 38.73), hasGrossValue("EUR", 38.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-11-21"), hasExDate("2011-11-21"), //
                        hasShares(32.445 - 0.074), //
                        hasSource("Quartalsbericht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.86), hasGrossValue("EUR", 4.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-08-19"), hasExDate("2011-08-19"), //
                        hasShares(53.782 - 1.580), //
                        hasSource("Quartalsbericht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 60.03), hasGrossValue("EUR", 60.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2011-12-30T00:00"), hasExDate("2011-12-30T00:00"), //
                                        hasShares(36.045), //
                                        hasSource("Quartalsbericht14.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 10th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2011-07-01"), hasExDate("2011-07-01"), //
                        hasShares(58.306 - 8.077 - 0.051), //
                        hasSource("Quartalsbericht14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.92), hasGrossValue("EUR", 1.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2011-12-31T00:00"), hasShares(0), //
                        hasSource("Quartalsbericht14.txt"), hasNote("Depotpreis 2011"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht15()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht15.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(9L));
        assertThat(countBuySell(results), is(7L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(5L));
        assertThat(countSkippedItems(results), is(5L));
        assertThat(results.size(), is(31));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0268059614"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-LiquiditätsPlan TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Pazifik"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009771980"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaBond TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2012-01-30"), hasShares(1.036), //
                        hasSource("Quartalsbericht15.txt"), hasNote(null), //
                        hasAmount("EUR", 1023.85), hasGrossValue("EUR", 1023.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2012-07-02T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht15.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-02-17"), hasShares(0.351), //
                        hasSource("Quartalsbericht15.txt"), hasNote(null), //
                        hasAmount("EUR", 17.08), hasGrossValue("EUR", 17.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2012-10-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht15.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2012-10-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht15.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-11-20"), hasShares(0.134), //
                        hasSource("Quartalsbericht15.txt"), hasNote(null), //
                        hasAmount("EUR", 58.50), hasGrossValue("EUR", 58.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2012-10-01T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht15.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2012-07-30"), hasShares(53.782), //
                        hasSource("Quartalsbericht15.txt"), hasNote(null), //
                        hasAmount("EUR", 2248.63), hasGrossValue("EUR", 2248.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2012-12-28T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht15.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-02-20"), hasShares(24.337), //
                        hasSource("Quartalsbericht15.txt"), hasNote(null), //
                        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-07-02"), hasShares(0.275), //
                        hasSource("Quartalsbericht15.txt"), hasNote(null), //
                        hasAmount("EUR", 10.95), hasGrossValue("EUR", 10.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-12-20"), hasShares(17.397), //
                        hasSource("Quartalsbericht15.txt"), hasNote(null), //
                        hasAmount("EUR", 750.00), hasGrossValue("EUR", 750.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2012-05-21"), hasShares(1.617), //
                        hasSource("Quartalsbericht15.txt"), hasNote("Zulagenzahlung 2011"), //
                        hasAmount("EUR", 62.86), hasGrossValue("EUR", 62.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2012-07-02T00:00"), hasExDate("2012-07-02T00:00"), //
                                        hasShares(15.795), //
                                        hasSource("Quartalsbericht15.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2012-02-17"), hasExDate("2012-02-17"), //
                        hasShares(34.333 - 0.351), //
                        hasSource("Quartalsbericht15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 17.08), hasGrossValue("EUR", 17.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2012-10-01T00:00"), hasExDate("2012-10-01T00:00"), //
                                        hasShares(15.216), //
                                        hasSource("Quartalsbericht15.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2012-10-01T00:00"), hasExDate("2012-10-01T00:00"), //
                                        hasShares(5.864), //
                                        hasSource("Quartalsbericht15.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 5th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2012-11-20"), hasExDate("2012-11-20"), //
                        hasShares(6.324 - 0.134), //
                        hasSource("Quartalsbericht15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 58.50), hasGrossValue("EUR", 58.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2012-10-01T00:00"), hasExDate("2012-10-01T00:00"), //
                                        hasShares(32.445), //
                                        hasSource("Quartalsbericht15.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2012-12-28T00:00"), hasExDate("2012-12-28T00:00"), //
                                        hasShares(36.045), //
                                        hasSource("Quartalsbericht15.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 8th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2012-07-02"), hasExDate("2012-07-02"), //
                        hasShares(101.932 - 17.397 - 0.275), //
                        hasSource("Quartalsbericht15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 10.95), hasGrossValue("EUR", 10.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2012-12-31T00:00"), hasShares(0), //
                        hasSource("Quartalsbericht15.txt"), hasNote("Depotpreis 2012"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht16()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht16.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(10L));
        assertThat(countAccountTransactions(results), is(11L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(14L));
        assertThat(results.size(), is(42));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLuxTeam-Aktien Asien"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2017-12-29T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht16.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-08-17"), hasShares(0.091), //
                        hasSource("Quartalsbericht16.txt"), hasNote(null), //
                        hasAmount("EUR", 25.16), hasGrossValue("EUR", 25.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-08-10"), hasShares(0.239), //
                        hasSource("Quartalsbericht16.txt"), hasNote(null), //
                        hasAmount("EUR", 16.38), hasGrossValue("EUR", 16.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-11-16"), hasShares(0.101), //
                        hasSource("Quartalsbericht16.txt"), hasNote(null), //
                        hasAmount("EUR", 14.27), hasGrossValue("EUR", 14.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-11-16"), hasShares(0.039), //
                        hasSource("Quartalsbericht16.txt"), hasNote(null), //
                        hasAmount("EUR", 5.50), hasGrossValue("EUR", 5.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-11-16"), hasShares(0.052), //
                        hasSource("Quartalsbericht16.txt"), hasNote(null), //
                        hasAmount("EUR", 33.91), hasGrossValue("EUR", 33.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-11-16"), hasShares(0.236), //
                        hasSource("Quartalsbericht16.txt"), hasNote(null), //
                        hasAmount("EUR", 27.16), hasGrossValue("EUR", 27.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-12-29"), hasShares(0.009), //
                        hasSource("Quartalsbericht16.txt"), hasNote(null), //
                        hasAmount("EUR", 1.13), hasGrossValue("EUR", 1.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-08-10"), hasShares(0.220), //
                        hasSource("Quartalsbericht16.txt"), hasNote(null), //
                        hasAmount("EUR", 28.53), hasGrossValue("EUR", 28.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-01-12"), hasShares(0.582), //
                        hasSource("Quartalsbericht16.txt"), hasNote(null), //
                        hasAmount("EUR", 42.80), hasGrossValue("EUR", 42.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-11-30"), hasShares(25.428), //
                        hasSource("Quartalsbericht16.txt"), hasNote(null), //
                        hasAmount("EUR", 1930.00), hasGrossValue("EUR", 1930.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2018-05-15"), hasShares(2.024), //
                        hasSource("Quartalsbericht16.txt"), hasNote("Zulagenzahlung 2017"), //
                        hasAmount("EUR", 154.00), hasGrossValue("EUR", 154.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2018-01-26T00:00"), hasExDate("2017-12-29T00:00"), //
                                        hasShares(0.00), //
                                        hasSource("Quartalsbericht16.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-08-17"), hasExDate("2018-08-17"), //
                        hasShares(16.642 - 0.091), //
                        hasSource("Quartalsbericht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.16), hasGrossValue("EUR", 25.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-08-10"), hasExDate("2018-08-10"), //
                        hasShares(39.231 - 0.239), //
                        hasSource("Quartalsbericht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.38), hasGrossValue("EUR", 16.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-11-16"), hasExDate("2018-11-16"), //
                        hasShares(15.615 - 0.101), //
                        hasSource("Quartalsbericht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.27), hasGrossValue("EUR", 14.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-11-16"), hasExDate("2018-11-16"), //
                        hasShares(6.018 - 0.039), //
                        hasSource("Quartalsbericht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.50), hasGrossValue("EUR", 5.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-11-16"), hasExDate("2018-11-16"), //
                        hasShares(6.833 - 0.052), //
                        hasSource("Quartalsbericht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.91), hasGrossValue("EUR", 33.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-11-16"), hasExDate("2018-11-16"), //
                        hasShares(34.621 - 0.236), //
                        hasSource("Quartalsbericht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 27.16), hasGrossValue("EUR", 27.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-01-12"), hasExDate("2017-12-29"), //
                        hasShares(0.00), //
                        hasSource("Quartalsbericht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.13), hasGrossValue("EUR", 1.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-08-10"), hasExDate("2018-08-10"), //
                        hasShares(37.758 - 0.220), //
                        hasSource("Quartalsbericht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 28.53), hasGrossValue("EUR", 28.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2018-12-31T00:00"), hasShares(0.00), //
                        hasSource("Quartalsbericht16.txt"), hasNote("Depotpreis 2018"), //
                        hasAmount("EUR", 12.50), hasGrossValue("EUR", 12.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2018-01-24"), hasExDate("2017-12-29"), //
                                        hasShares(0.00), //
                                        hasSource("Quartalsbericht16.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2018-01-12"), hasExDate("2017-12-29"), //
                                        hasShares(0.00), //
                                        hasSource("Quartalsbericht16.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-01-12"), hasExDate("2018-01-12"), //
                        hasShares(267.479), //
                        hasSource("Quartalsbericht16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 42.80), hasGrossValue("EUR", 42.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht17()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht17.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(10L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(26));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLuxTeam-Aktien Asien"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-16"), hasShares(0.240), //
                        hasSource("Quartalsbericht17.txt"), hasNote(null), //
                        hasAmount("EUR", 67.40), hasGrossValue("EUR", 67.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-22"), hasShares(0.396), //
                        hasSource("Quartalsbericht17.txt"), hasNote(null), //
                        hasAmount("EUR", 25.50), hasGrossValue("EUR", 25.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-15"), hasShares(0.337), //
                        hasSource("Quartalsbericht17.txt"), hasNote(null), //
                        hasAmount("EUR", 58.24), hasGrossValue("EUR", 58.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-15"), hasShares(0.130), //
                        hasSource("Quartalsbericht17.txt"), hasNote(null), //
                        hasAmount("EUR", 22.45), hasGrossValue("EUR", 22.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-15"), hasShares(0.021), //
                        hasSource("Quartalsbericht17.txt"), hasNote(null), //
                        hasAmount("EUR", 15.44), hasGrossValue("EUR", 15.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-15"), hasShares(0.105), //
                        hasSource("Quartalsbericht17.txt"), hasNote(null), //
                        hasAmount("EUR", 13.50), hasGrossValue("EUR", 13.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-02-22"), hasShares(0.125), //
                        hasSource("Quartalsbericht17.txt"), hasNote(null), //
                        hasAmount("EUR", 14.73), hasGrossValue("EUR", 14.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-06-25"), hasShares(0.051), //
                        hasSource("Quartalsbericht17.txt"), hasNote(null), //
                        hasAmount("EUR", 3.99), hasGrossValue("EUR", 3.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-10"), hasShares(0.058), //
                        hasSource("Quartalsbericht17.txt"), hasNote(null), //
                        hasAmount("EUR", 4.92), hasGrossValue("EUR", 4.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-13"), hasShares(21.727), //
                        hasSource("Quartalsbericht17.txt"), hasNote(null), //
                        hasAmount("EUR", 1930.00), hasGrossValue("EUR", 1930.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2019-05-16"), hasShares(2.197), //
                        hasSource("Quartalsbericht17.txt"), hasNote("Zulagenzahlung 2018"), //
                        hasAmount("EUR", 175.00), hasGrossValue("EUR", 175.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-08-16"), hasExDate("2019-08-16"), //
                        hasShares(16.882 - 0.240), //
                        hasSource("Quartalsbericht17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 67.40), hasGrossValue("EUR", 67.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-02-22"), hasExDate("2019-02-22"), //
                        hasShares(39.627 - 0.396), //
                        hasSource("Quartalsbericht17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.50), hasGrossValue("EUR", 25.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-15"), hasExDate("2019-11-15"), //
                        hasShares(15.952 - 0.337), //
                        hasSource("Quartalsbericht17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 58.24), hasGrossValue("EUR", 58.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-15"), hasExDate("2019-11-15"), //
                        hasShares(6.148 - 0.130), //
                        hasSource("Quartalsbericht17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 22.45), hasGrossValue("EUR", 22.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-15"), hasExDate("2019-11-15"), //
                        hasShares(6.854 - 0.021), //
                        hasSource("Quartalsbericht17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 15.44), hasGrossValue("EUR", 15.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-11-15"), hasExDate("2019-11-15"), //
                        hasShares(34.726 - 0.105), //
                        hasSource("Quartalsbericht17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.50), hasGrossValue("EUR", 13.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-02-22"), hasExDate("2019-02-22"), //
                        hasShares(37.883 - 0.125), //
                        hasSource("Quartalsbericht17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.73), hasGrossValue("EUR", 14.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2019-12-31"), hasAmount("EUR", 12.50), //
                        hasSource("Quartalsbericht17.txt"), hasNote("Depotpreis 2019"))));
    }

    @Test
    public void testQuartalsbericht18()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht18.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(14L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(7L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(34));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008474511"), hasWkn(null), hasTicker(null), //
                        hasName("AriDeka CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0133666759"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ConvergenceAktien TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLuxTeam-Aktien Asien"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005424519"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BR 100"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-19"), hasShares(0.882), //
                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                        hasAmount("EUR", 232.27), hasGrossValue("EUR", 232.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-21"), hasShares(0.761), //
                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                        hasAmount("EUR", 56.27), hasGrossValue("EUR", 56.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-18"), hasShares(0.388), //
                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                        hasAmount("EUR", 18.49), hasGrossValue("EUR", 18.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-18"), hasShares(0.952), //
                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                        hasAmount("EUR", 110.18), hasGrossValue("EUR", 110.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-18"), hasShares(0.148), //
                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                        hasAmount("EUR", 17.13), hasGrossValue("EUR", 17.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-19"), hasShares(0.854), //
                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                        hasAmount("EUR", 494.76), hasGrossValue("EUR", 494.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-18"), hasShares(0.726), //
                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                        hasAmount("EUR", 58.09), hasGrossValue("EUR", 58.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-21"), hasShares(0.018), //
                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                        hasAmount("EUR", 2.65), hasGrossValue("EUR", 2.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-18"), hasShares(0.901), //
                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                        hasAmount("EUR", 79.92), hasGrossValue("EUR", 79.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-05"), hasShares(25.119), //
                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                        hasAmount("EUR", 1977.86), hasGrossValue("EUR", 1977.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-24"), hasShares(275.911), //
                        hasSource("Quartalsbericht18.txt"), hasNote("Schädliche Verwendung"), //
                        hasAmount("EUR", 23190.32), hasGrossValue("EUR", 23190.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-24"), hasShares(0.580), //
                        hasSource("Quartalsbericht18.txt"), hasNote("Entgelt Auflösung"), //
                        hasAmount("EUR", 48.74), hasGrossValue("EUR", 48.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2020-05-15"), hasShares(2.212), //
                        hasSource("Quartalsbericht18.txt"), hasNote("Zulagenzahlung 2019"), //
                        hasAmount("EUR", 175.00), hasGrossValue("EUR", 175.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-11-24"), hasShares(1.356), //
                        hasSource("Quartalsbericht18.txt"), hasNote("Korrekturbuchung"), //
                        hasAmount("EUR", 114.00), hasGrossValue("EUR", 114.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-11-24"), hasShares(45.699), //
                        hasSource("Quartalsbericht18.txt"), hasNote("Steuerrückzahlung"), //
                        hasAmount("EUR", 3841.00), hasGrossValue("EUR", 3841.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-11-24"), hasShares(23.331), //
                        hasSource("Quartalsbericht18.txt"), hasNote("Zulagenrückzahlung"), //
                        hasAmount("EUR", 1960.93), hasGrossValue("EUR", 1960.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        fee( //
                                        hasDate("2020-12-31T00:00"), hasShares(0.00), //
                                        hasSource("Quartalsbericht18.txt"), //
                                        hasNote("Depotpreis 2020"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-11-24T00:00"), hasShares(0.00), //
                        hasSource("Quartalsbericht18.txt"), hasNote("Entgelt Auflösung"), //
                        hasAmount("EUR", 48.74), hasGrossValue("EUR", 48.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-02-21"), hasExDate("2020-02-21"), //
                        hasShares(39.627), //
                        hasSource("Quartalsbericht18.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 56.27), hasGrossValue("EUR", 56.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-02-21"), hasExDate("2020-02-21"), //
                        hasShares(37.883), //
                        hasSource("Quartalsbericht18.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.65), hasGrossValue("EUR", 2.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(16), //
                                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(40), //
                                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(15), //
                                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(6), //
                                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(6), //
                                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(34), //
                                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2020-03-18"), hasShares(37), //
                                        hasSource("Quartalsbericht18.txt"), hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testQuartalsbericht19()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht19.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(16L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(22));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2CFT3"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BasisAnlage A100"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2J6P1"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-RentenStrategie Global CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0230155797"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Renten konservativ"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-16"), hasShares(0.220), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-18"), hasShares(0.216), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-16"), hasShares(0.213), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-22"), hasShares(0.471), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 42.00), hasGrossValue("EUR", 42.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-04-16"), hasShares(7.116), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 340.80), hasGrossValue("EUR", 340.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-16"), hasShares(3.786), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 340.80), hasGrossValue("EUR", 340.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-04-17"), hasShares(0.010), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 0.50), hasGrossValue("EUR", 0.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-17"), hasShares(0.006), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 0.50), hasGrossValue("EUR", 0.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-05-18"), hasShares(14.209), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 682.17), hasGrossValue("EUR", 682.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-18"), hasShares(7.436), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 682.17), hasGrossValue("EUR", 682.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-09"), hasShares(7.731), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 373.94), hasGrossValue("EUR", 373.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-09"), hasShares(4.077), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 373.94), hasGrossValue("EUR", 373.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-10"), hasShares(0.007), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 0.66), hasGrossValue("EUR", 0.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-10"), hasShares(0.014), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 0.66), hasGrossValue("EUR", 0.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-30"), hasShares(7.996), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 386.77), hasGrossValue("EUR", 386.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-30"), hasShares(4.205), //
                        hasSource("Quartalsbericht19.txt"), hasNote(null), //
                        hasAmount("EUR", 386.77), hasGrossValue("EUR", 386.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2020-05-19"), hasShares(1.422), //
                        hasSource("Quartalsbericht19.txt"), hasNote("Zulagenzahlung 2019"), //
                        hasAmount("EUR", 68.25), hasGrossValue("EUR", 68.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2020-05-19"), hasShares(1.126), //
                        hasSource("Quartalsbericht19.txt"), hasNote("Zulagenzahlung 2019"), //
                        hasAmount("EUR", 106.75), hasGrossValue("EUR", 106.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-22"), hasExDate("2020-05-22"), //
                        hasShares(21.100 - 0.471), //
                        hasSource("Quartalsbericht19.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 42.00), hasGrossValue("EUR", 42.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht20()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht20.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(9L));
        assertThat(countBuySell(results), is(19L));
        assertThat(countAccountTransactions(results), is(11L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(39));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0350482435"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLuxTeam-EmergingMarkets"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2CFT3"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BasisAnlage offensiv"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLuxTeam-Aktien Asien"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052863874"), hasWkn(null), hasTicker(null), //
                        hasName("Euro"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786186"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaSelect CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2CDS0"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-DividendenStrategie CF (A)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2J6P1"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-RentenStrategie Global CF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-21"), hasShares(0.006), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 1.87), hasGrossValue("EUR", 1.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-21"), hasShares(0.167), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 22.47), hasGrossValue("EUR", 22.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-16"), hasShares(0.201), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-18"), hasShares(0.200), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-16"), hasShares(0.197), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-08"), hasShares(6.143), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 4751.73), hasGrossValue("EUR", 4751.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-26"), hasShares(0.260), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 12.31), hasGrossValue("EUR", 12.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-18"), hasShares(9.867), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 1192.43), hasGrossValue("EUR", 1192.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-17"), hasShares(25.910), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 1906.72), hasGrossValue("EUR", 1906.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24"), hasShares(0.013), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Vertragspreis 2020"), //
                        hasAmount("EUR", 1.95), hasGrossValue("EUR", 1.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24"), hasShares(1.560), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Schädliche Verwendung"), //
                        hasAmount("EUR", 231.63), hasGrossValue("EUR", 231.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24"), hasShares(0.066), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Entgelt Auflösung"), //
                        hasAmount("EUR", 9.75), hasGrossValue("EUR", 9.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24"), hasShares(0.083), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Vertragspreis 2020"), //
                        hasAmount("EUR", 7.79), hasGrossValue("EUR", 7.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24"), hasShares(9.869), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Schädliche Verwendung"), //
                        hasAmount("EUR", 925.32), hasGrossValue("EUR", 925.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24"), hasShares(0.416), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Entgelt Auflösung"), //
                        hasAmount("EUR", 38.99), hasGrossValue("EUR", 38.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-07-24"), hasShares(4.212), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 391.83), hasGrossValue("EUR", 391.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-24"), hasShares(2.661), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 391.83), hasGrossValue("EUR", 391.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 18th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-07-27"), hasShares(0.010), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 0.93), hasGrossValue("EUR", 0.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 19th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-27"), hasShares(0.006), //
                        hasSource("Quartalsbericht20.txt"), hasNote(null), //
                        hasAmount("EUR", 0.93), hasGrossValue("EUR", 0.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2020-08-19"), hasShares(0.260), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Kauf aus Steuererstattung"), //
                        hasAmount("EUR", 12.29), hasGrossValue("EUR", 12.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-08-24"), hasShares(0.287), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Steuerrückzahlung"), //
                        hasAmount("EUR", 42.60), hasGrossValue("EUR", 42.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-08-24"), hasShares(0.741), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Zulagenrückzahlung"), //
                        hasAmount("EUR", 110.00), hasGrossValue("EUR", 110.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-08-24"), hasShares(1.817), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Steuerrückzahlung"), //
                        hasAmount("EUR", 170.40), hasGrossValue("EUR", 170.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-08-24"), hasShares(4.693), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Zulagenrückzahlung"), //
                        hasAmount("EUR", 440.00), hasGrossValue("EUR", 440.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-21"), hasExDate("2020-08-21"), //
                        hasShares(14.513 - 0.006), //
                        hasSource("Quartalsbericht20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.87), hasGrossValue("EUR", 1.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-21"), hasExDate("2020-08-21"), //
                        hasShares(28.345 - 0.167), //
                        hasSource("Quartalsbericht20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 22.47), hasGrossValue("EUR", 22.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-08-24"), hasAmount("EUR", 1.95), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Vertragspreis 2020"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-08-24"), hasAmount("EUR", 9.75), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Entgelt Auflösung"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-08-24"), hasAmount("EUR", 7.79), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Vertragspreis 2020"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-08-24"), hasAmount("EUR", 38.99), //
                        hasSource("Quartalsbericht20.txt"), hasNote("Entgelt Auflösung"))));
    }

    @Test
    public void testQuartalsbericht21()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht21.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(11L));
        assertThat(countBuySell(results), is(55L));
        assertThat(countAccountTransactions(results), is(19L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(85));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0350482435"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLuxTeam-EmergingMarkets"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2CFT3"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-BasisAnlage offensiv"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052859252"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLuxTeam-Aktien Asien"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0052863874"), hasWkn(null), hasTicker(null), //
                        hasName("Euro"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0062624902"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-Deutschland TF A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786186"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaSelect CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009786285"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-EuropaPotential TF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2J6P1"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-RentenStrategie Global CF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2CDS0"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-DividendenStrategie CF (A)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0230155797"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Renten konservativ"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-21"), hasShares(0.006), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 1.87), hasGrossValue("EUR", 1.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-21"), hasShares(0.167), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 22.47), hasGrossValue("EUR", 22.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-16"), hasShares(0.188), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-18"), hasShares(0.183), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-17"), hasShares(0.247), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-16"), hasShares(0.220), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-18"), hasShares(0.216), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-16"), hasShares(0.213), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-16"), hasShares(0.201), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-18"), hasShares(0.200), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-16"), hasShares(0.197), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-16"), hasShares(0.191), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-13"), hasShares(0.003), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 0.52), hasGrossValue("EUR", 0.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-17"), hasShares(0.185), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-16"), hasShares(0.185), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 36.00), hasGrossValue("EUR", 36.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-08"), hasShares(6.143), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 4751.73), hasGrossValue("EUR", 4751.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-26"), hasShares(0.260), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 12.31), hasGrossValue("EUR", 12.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 18th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-18"), hasShares(9.867), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 1192.43), hasGrossValue("EUR", 1192.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 19th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-21"), hasShares(0.115), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 8.77), hasGrossValue("EUR", 8.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 20th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-17"), hasShares(25.910), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 1906.72), hasGrossValue("EUR", 1906.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 21th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-21"), hasShares(0.016), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 2.37), hasGrossValue("EUR", 2.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 22th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-21"), hasShares(0.004), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 0.62), hasGrossValue("EUR", 0.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 23th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-22"), hasShares(0.471), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 42.00), hasGrossValue("EUR", 42.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 24th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24"), hasShares(0.013), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Vertragspreis 2020"), //
                        hasAmount("EUR", 1.95), hasGrossValue("EUR", 1.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 25th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24"), hasShares(1.560), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Schädliche Verwendung"), //
                        hasAmount("EUR", 231.63), hasGrossValue("EUR", 231.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 26th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24"), hasShares(0.066), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Entgelt Auflösung"), //
                        hasAmount("EUR", 9.75), hasGrossValue("EUR", 9.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 27th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24"), hasShares(0.083), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Vertragspreis 2020"), //
                        hasAmount("EUR", 7.79), hasGrossValue("EUR", 7.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 28th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24"), hasShares(9.869), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Schädliche Verwendung"), //
                        hasAmount("EUR", 925.32), hasGrossValue("EUR", 925.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 29th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-24"), hasShares(0.416), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Entgelt Auflösung"), //
                        hasAmount("EUR", 38.99), hasGrossValue("EUR", 38.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 30th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-11"), hasShares(5.532), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 776.10), hasGrossValue("EUR", 776.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 31th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-11"), hasShares(8.102), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 776.10), hasGrossValue("EUR", 776.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 32th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-12"), hasShares(2.668), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 354.90), hasGrossValue("EUR", 354.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 33th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-12"), hasShares(3.757), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 354.90), hasGrossValue("EUR", 354.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 34th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-13"), hasShares(0.024), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 3.05), hasGrossValue("EUR", 3.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 35th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-13"), hasShares(0.033), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 3.05), hasGrossValue("EUR", 3.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 36th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-16"), hasShares(5.248), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 650.44), hasGrossValue("EUR", 650.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 37th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-16"), hasShares(7.102), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 650.44), hasGrossValue("EUR", 650.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 38th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-17"), hasShares(18.994), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 1718.96), hasGrossValue("EUR", 1718.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 39h buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-17"), hasShares(35.626), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 1718.96), hasGrossValue("EUR", 1718.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 40th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-04-16"), hasShares(7.116), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 340.80), hasGrossValue("EUR", 340.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 41th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-16"), hasShares(3.786), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 340.80), hasGrossValue("EUR", 340.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 42th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-04-17"), hasShares(0.010), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 0.50), hasGrossValue("EUR", 0.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 43th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-17"), hasShares(0.006), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 0.50), hasGrossValue("EUR", 0.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 44th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-05-18"), hasShares(14.209), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 682.17), hasGrossValue("EUR", 682.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 45th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-18"), hasShares(7.436), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 682.17), hasGrossValue("EUR", 682.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 46th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-09"), hasShares(7.731), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 373.94), hasGrossValue("EUR", 373.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 47th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-09"), hasShares(4.077), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 373.94), hasGrossValue("EUR", 373.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 48th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-10"), hasShares(0.007), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 0.66), hasGrossValue("EUR", 0.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 49th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-10"), hasShares(0.014), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 0.66), hasGrossValue("EUR", 0.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 50th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-30"), hasShares(7.996), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 386.77), hasGrossValue("EUR", 386.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 51th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-30"), hasShares(4.205), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 386.77), hasGrossValue("EUR", 386.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 55th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-07-24"), hasShares(4.212), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 391.83), hasGrossValue("EUR", 391.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 53th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-24"), hasShares(2.661), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 391.83), hasGrossValue("EUR", 391.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 54th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-07-27"), hasShares(0.010), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 0.93), hasGrossValue("EUR", 0.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 55th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-27"), hasShares(0.006), //
                        hasSource("Quartalsbericht21.txt"), hasNote(null), //
                        hasAmount("EUR", 0.93), hasGrossValue("EUR", 0.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2020-08-19"), hasShares(0.260), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Kauf aus Steuererstattung"), //
                        hasAmount("EUR", 12.29), hasGrossValue("EUR", 12.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2020-05-19"), hasShares(1.422), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Zulagenzahlung 2019"), //
                        hasAmount("EUR", 68.25), hasGrossValue("EUR", 68.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2020-05-19"), hasShares(1.126), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Zulagenzahlung 2019"), //
                        hasAmount("EUR", 106.75), hasGrossValue("EUR", 106.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-08-24"), hasShares(0.287), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Steuerrückzahlung"), //
                        hasAmount("EUR", 42.60), hasGrossValue("EUR", 42.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-08-24"), hasShares(0.741), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Zulagenrückzahlung"), //
                        hasAmount("EUR", 110.00), hasGrossValue("EUR", 110.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-08-24"), hasShares(1.817), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Steuerrückzahlung"), //
                        hasAmount("EUR", 170.40), hasGrossValue("EUR", 170.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check delivery outbound (Auslieferung) transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2020-08-24"), hasShares(4.693), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Zulagenrückzahlung"), //
                        hasAmount("EUR", 440.00), hasGrossValue("EUR", 440.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-21"), hasExDate("2020-08-21"), //
                        hasShares(14.513 - 0.006), //
                        hasSource("Quartalsbericht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.87), hasGrossValue("EUR", 1.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-21"), hasExDate("2020-08-21"), //
                        hasShares(28.345 - 0.167), //
                        hasSource("Quartalsbericht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 22.47), hasGrossValue("EUR", 22.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-13"), hasExDate("2020-11-13"), //
                        hasShares(7.377 - 0.185 - 0.185 - 0.003), //
                        hasSource("Quartalsbericht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.52), hasGrossValue("EUR", 0.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-02-21"), hasExDate("2020-02-21"), //
                        hasShares(0.000 + 25.910 - 0.115), //
                        hasSource("Quartalsbericht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.77), hasGrossValue("EUR", 8.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-02-21"), hasExDate("2020-02-21"), //
                        hasShares(33.823 - 0.016), //
                        hasSource("Quartalsbericht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.37), hasGrossValue("EUR", 2.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-02-21"), hasExDate("2020-02-21"), //
                        hasShares(8.848 - 0.004), //
                        hasSource("Quartalsbericht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.62), hasGrossValue("EUR", 0.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-22"), hasExDate("2020-05-22"), //
                        hasShares(0.000 + 4.693 + 1.817 + 0.416 + 9.869 + 0.083 - 0.471), //
                        hasSource("Quartalsbericht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 42.00), hasGrossValue("EUR", 42.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-12-31"), hasAmount("EUR", 12.18), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Depotpreis 2020"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-08-24"), hasAmount("EUR", 1.95), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Vertragspreis 2020"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-08-24"), hasAmount("EUR", 9.75), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Entgelt Auflösung"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-08-24"), hasAmount("EUR", 7.79), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Vertragspreis 2020"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-08-24"), hasAmount("EUR", 38.99), //
                        hasSource("Quartalsbericht21.txt"), hasNote("Entgelt Auflösung"))));
    }

    @Test
    public void testQuartalsbericht22()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht22.txt"), errors);

        // Filter securities
        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

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
                        hasIsin("LU0348413815"), hasWkn(null), hasTicker(null), //
                        hasName("DekaLux-PharmaTech TF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2008-06-30"), hasShares(12.888), //
                        hasSource("Quartalsbericht22.txt"), hasNote(null), //
                        hasAmount("EUR", 1225.74), hasGrossValue("EUR", 1225.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2008-12-12"), hasShares(0.110), //
                        hasSource("Quartalsbericht22.txt"), hasNote("Depotpreis 2008"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2008-12-12"), hasAmount("EUR", 10.00), //
                        hasSource("Quartalsbericht22.txt"), hasNote("Depotpreis 2008"))));
    }

    @Test
    public void testQuartalsbericht23()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(17L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0230155797"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Renten konservativ"), //
                        hasCurrencyCode("EUR"))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2J6P1"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-RentenStrategie Global CF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-15"), hasShares(0.739), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 35.40), hasGrossValue("EUR", 35.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-15"), hasShares(0.265), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 24.60), hasGrossValue("EUR", 24.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-22"), hasExDate("2020-05-22"), //
                        hasShares(8.083 - 0.180), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 16.10), hasGrossValue("EUR", 16.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-22"), hasShares(0.180), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 16.10), hasGrossValue("EUR", 16.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-04-15"), hasShares(5.460), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 261.53), hasGrossValue("EUR", 261.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-15"), hasShares(2.907), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 261.53), hasGrossValue("EUR", 261.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-04-16"), hasShares(0.016), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 0.77), hasGrossValue("EUR", 0.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-16"), hasShares(0.009), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 0.77), hasGrossValue("EUR", 0.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-05-13"), hasShares(2.968), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 142.51), hasGrossValue("EUR", 142.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-13"), hasShares(1.556), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 142.51), hasGrossValue("EUR", 142.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-05-14"), hasShares(0.002), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 0.21), hasGrossValue("EUR", 0.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-14"), hasShares(0.004), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 0.21), hasGrossValue("EUR", 0.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-08"), hasShares(2.953), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 142.79), hasGrossValue("EUR", 142.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-08"), hasShares(1.562), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 142.79), hasGrossValue("EUR", 142.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-09"), hasShares(0.00041348), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-09"), hasShares(0.00021808), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-26"), hasShares(3.056), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 147.82), hasGrossValue("EUR", 147.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 18th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-26"), hasShares(1.606), //
                        hasSource("Quartalsbericht23.txt"), hasNote(null), //
                        hasAmount("EUR", 147.82), hasGrossValue("EUR", 147.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht24()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(90L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(98));
        new AssertImportActions().check(results, "EUR");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0230155797"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-Renten konservativ"), //
                        hasCurrencyCode("EUR"))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2CDS0"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-DividendenStrategie CF (A)"), //
                        hasCurrencyCode("EUR"))));

        // check 3rd security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2J6P1"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-RentenStrategie Global CF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-15"), hasShares(0.739), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 35.40), hasGrossValue("EUR", 35.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-15"), hasShares(0.265), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 24.60), hasGrossValue("EUR", 24.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-22"), hasShares(0.180), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 16.10), hasGrossValue("EUR", 16.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-22"), hasExDate("2020-05-22"), //
                        hasShares(0.00 + 4.128 - 0.180), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 16.10), hasGrossValue("EUR", 16.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2020-08-18"), hasShares(1.800), //
                        hasSource("Quartalsbericht24.txt"), hasNote("Zulagenzahlung 2019"), //
                        hasAmount("EUR", 276.75), hasGrossValue("EUR", 276.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2020-08-18"), hasShares(4.128), //
                        hasSource("Quartalsbericht24.txt"), hasNote("Zulagenzahlung 2019"), //
                        hasAmount("EUR", 398.25), hasGrossValue("EUR", 398.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-13"), hasShares(0.122), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 18.47), hasGrossValue("EUR", 18.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-13"), hasExDate("2020-11-13"), //
                        hasShares(9.359 + 5.036 - 5.036 - 0.122), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 18.47), hasGrossValue("EUR", 18.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-01"), hasShares(5.036), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 805.00), hasGrossValue("EUR", 805.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage(Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        sale( //
                                        hasDate("2020-12-01"), hasShares(5.036), //
                                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                                        hasAmount("EUR", 805.00), hasGrossValue("EUR", 805.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-03"), hasShares(2.741), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 425.21), hasGrossValue("EUR", 425.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-03"), hasShares(4.341), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 425.21), hasGrossValue("EUR", 425.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-04"), hasShares(0.881), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 135.01), hasGrossValue("EUR", 135.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-04"), hasShares(1.374), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 135.01), hasGrossValue("EUR", 135.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-06"), hasShares(1.328), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 130.26), hasGrossValue("EUR", 130.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-06"), hasShares(0.867), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 130.26), hasGrossValue("EUR", 130.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-09"), hasShares(0.009), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 1.23), hasGrossValue("EUR", 1.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-09"), hasShares(0.013), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 1.23), hasGrossValue("EUR", 1.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-11"), hasShares(1.885), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 264.47), hasGrossValue("EUR", 264.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 16th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-11"), hasShares(2.761), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 264.47), hasGrossValue("EUR", 264.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 17th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-12"), hasShares(2.909), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 274.76), hasGrossValue("EUR", 274.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 18th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-12"), hasShares(5.658), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 274.76), hasGrossValue("EUR", 274.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 19th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-13"), hasShares(0.008), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.38), hasGrossValue("EUR", 0.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 20th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-13"), hasShares(0.004), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.38), hasGrossValue("EUR", 0.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 21th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-16"), hasShares(4.256), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 389.76), hasGrossValue("EUR", 389.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 22th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-16"), hasShares(8.060), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 389.76), hasGrossValue("EUR", 389.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 23th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-04-15"), hasShares(5.460), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 261.53), hasGrossValue("EUR", 261.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 24th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-15"), hasShares(2.907), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 261.53), hasGrossValue("EUR", 261.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 25th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-04-16"), hasShares(0.016), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.77), hasGrossValue("EUR", 0.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 26th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-16"), hasShares(0.009), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.77), hasGrossValue("EUR", 0.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 27th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-05-13"), hasShares(2.968), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 142.51), hasGrossValue("EUR", 142.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 28th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-13"), hasShares(1.556), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 142.51), hasGrossValue("EUR", 142.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 29th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-05-14"), hasShares(0.002), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.21), hasGrossValue("EUR", 0.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 30th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-14"), hasShares(0.004), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.21), hasGrossValue("EUR", 0.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 31th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-08"), hasShares(2.953), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 142.79), hasGrossValue("EUR", 142.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 32th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-08"), hasShares(1.562), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 142.79), hasGrossValue("EUR", 142.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 33th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-09"), hasShares(0.00041348), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 34th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-09"), hasShares(0.00021808), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 35th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-26"), hasShares(3.056), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 147.82), hasGrossValue("EUR", 147.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 36th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-26"), hasShares(1.606), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 147.82), hasGrossValue("EUR", 147.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 37th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-07-24"), hasShares(1.613), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 150.10), hasGrossValue("EUR", 150.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 38th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-24"), hasShares(1.019), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 150.10), hasGrossValue("EUR", 150.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 39th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-07-27"), hasShares(0.004), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.36), hasGrossValue("EUR", 0.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 40th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-27"), hasShares(0.002), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.36), hasGrossValue("EUR", 0.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 41th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-18"), hasShares(1.621), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 151.81), hasGrossValue("EUR", 151.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 42th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-18"), hasShares(1.024), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 151.81), hasGrossValue("EUR", 151.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 43th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-19"), hasShares(0.00020347), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 44th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-19"), hasShares(0.00032014), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 45th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-20"), hasShares(2.949), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 276.43), hasGrossValue("EUR", 276.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 46th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-20"), hasShares(1.878), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 276.43), hasGrossValue("EUR", 276.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 47th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-08-21"), hasShares(0.014), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 1.32), hasGrossValue("EUR", 1.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 48th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-21"), hasShares(0.009), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 1.32), hasGrossValue("EUR", 1.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 49th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-07"), hasShares(2.970), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 279.61), hasGrossValue("EUR", 279.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 50th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-07"), hasShares(1.897), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 279.61), hasGrossValue("EUR", 279.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 51th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-09"), hasShares(1.890), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 277.99), hasGrossValue("EUR", 277.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 52th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-09"), hasShares(2.954), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 277.99), hasGrossValue("EUR", 277.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 53th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-10"), hasShares(0.005), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.75), hasGrossValue("EUR", 0.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 54th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-10"), hasShares(0.008), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.75), hasGrossValue("EUR", 0.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 55th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-18"), hasShares(2.985), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 281.35), hasGrossValue("EUR", 281.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 56th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-18"), hasShares(1.895), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 281.35), hasGrossValue("EUR", 281.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 57th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-21"), hasShares(0.001), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 58th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-21"), hasShares(0.001), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 59th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-22"), hasShares(1.945), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 282.46), hasGrossValue("EUR", 282.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 60th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-22"), hasShares(3.004), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 282.46), hasGrossValue("EUR", 282.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 61th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-23"), hasShares(0.085), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 7.94), hasGrossValue("EUR", 7.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 62th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-23"), hasShares(0.054), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 7.94), hasGrossValue("EUR", 7.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 63th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-28"), hasShares(2.989), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 279.25), hasGrossValue("EUR", 279.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 64th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-28"), hasShares(1.912), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 279.25), hasGrossValue("EUR", 279.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 65th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-29"), hasShares(0.002), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.32), hasGrossValue("EUR", 0.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 66th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-29"), hasShares(0.003), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.32), hasGrossValue("EUR", 0.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 67th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-10-08"), hasShares(2.951), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 276.74), hasGrossValue("EUR", 276.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 68th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-08"), hasShares(1.864), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 276.74), hasGrossValue("EUR", 276.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 69th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-10-23"), hasShares(1.900), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 278.84), hasGrossValue("EUR", 278.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 70th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-23"), hasShares(2.965), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 278.84), hasGrossValue("EUR", 278.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 71th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-10-26"), hasShares(0.023), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 2.20), hasGrossValue("EUR", 2.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 72th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-26"), hasShares(0.015), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 2.20), hasGrossValue("EUR", 2.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 73th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-10-27"), hasShares(2.942), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 276.70), hasGrossValue("EUR", 276.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 74th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-27"), hasShares(1.914), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 276.70), hasGrossValue("EUR", 276.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 75th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-10-29"), hasShares(1.970), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 275.86), hasGrossValue("EUR", 275.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 76th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-29"), hasShares(2.929), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 275.86), hasGrossValue("EUR", 275.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 77th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-10-30"), hasShares(1.906), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 267.96), hasGrossValue("EUR", 267.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 78th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-30"), hasShares(2.848), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 267.96), hasGrossValue("EUR", 267.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 79th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-02"), hasShares(1.820), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 257.62), hasGrossValue("EUR", 257.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 80th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-02"), hasShares(2.739), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 257.62), hasGrossValue("EUR", 257.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 81th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-04"), hasShares(2.790), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 262.87), hasGrossValue("EUR", 262.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 82th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-04"), hasShares(1.807), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 262.87), hasGrossValue("EUR", 262.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 83th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-05"), hasShares(0.022), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 3.20), hasGrossValue("EUR", 3.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 84th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-05"), hasShares(0.034), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 3.20), hasGrossValue("EUR", 3.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 85th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-09"), hasShares(2.851), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 270.31), hasGrossValue("EUR", 270.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 86th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-09"), hasShares(1.819), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 270.31), hasGrossValue("EUR", 270.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 87th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-10"), hasShares(0.001), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 88th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-10"), hasShares(0.001), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 89th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-12"), hasShares(2.910), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 276.42), hasGrossValue("EUR", 276.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 90th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-12"), hasShares(1.790), //
                        hasSource("Quartalsbericht24.txt"), hasNote(null), //
                        hasAmount("EUR", 276.42), hasGrossValue("EUR", 276.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        fee( //
                                        hasDate("2020-12-31"), hasShares(0.00), //
                                        hasSource("Quartalsbericht24.txt"), //
                                        hasNote("Depotpreis 2020"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testQuartalsbericht25()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht25.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK2CDS0"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-DividendenStrategie CF (A)"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-31"), hasShares(0.215), //
                        hasSource("Quartalsbericht25.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-28"), hasShares(0.212), //
                        hasSource("Quartalsbericht25.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-03-30"), hasShares(0.216), //
                        hasSource("Quartalsbericht25.txt"), hasNote(null), //
                        hasAmount("EUR", 40.00), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-01-02"), hasShares(2.695), //
                        hasSource("Quartalsbericht25.txt"), hasNote(null), //
                        hasAmount("EUR", 475.00), hasGrossValue("EUR", 475.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht26()
    {
        var extractor = new DekaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht26.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(14L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(21));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009807016"), hasWkn(null), hasTicker(null), //
                        hasName("hausInvest"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE0009809566"), hasWkn(null), hasTicker(null), //
                        hasName("Deka-ImmobilienEuropa"), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasDate("2020-06-16"), hasShares(0.00), //
                                        hasSource("Quartalsbericht26.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-17"), hasShares(2.566), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 108.45), hasGrossValue("EUR", 108.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-06"), hasShares(6.641), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 333.00), hasGrossValue("EUR", 333.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-10"), hasShares(0.720), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 33.38), hasGrossValue("EUR", 33.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-05"), hasShares(6.801), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 333.00), hasGrossValue("EUR", 333.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-05"), hasShares(6.786), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 333.00), hasGrossValue("EUR", 333.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-06"), hasShares(6.771), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 333.00), hasGrossValue("EUR", 333.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-05"), hasShares(6.760), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 333.00), hasGrossValue("EUR", 333.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-05"), hasShares(6.749), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 333.00), hasGrossValue("EUR", 333.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-06"), hasShares(6.733), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 333.00), hasGrossValue("EUR", 333.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 11st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-05"), hasShares(6.720), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 333.00), hasGrossValue("EUR", 333.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-07"), hasShares(6.719), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 333.00), hasGrossValue("EUR", 333.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 13st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-05"), hasShares(6.708), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 333.00), hasGrossValue("EUR", 333.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 14st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-05"), hasShares(6.688), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 333.00), hasGrossValue("EUR", 333.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 15st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-07"), hasShares(6.676), //
                        hasSource("Quartalsbericht26.txt"), hasNote(null), //
                        hasAmount("EUR", 333.00), hasGrossValue("EUR", 333.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-18"), hasExDate("2020-06-17"), //
                        hasShares(0.00), //
                        hasSource("Quartalsbericht26.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 108.45), hasGrossValue("EUR", 108.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-01-10"), hasExDate("2020-01-10"), //
                        hasShares(26.702), //
                        hasSource("Quartalsbericht26.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.38), hasGrossValue("EUR", 33.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-12-31"), hasShares(0), //
                        hasSource("Quartalsbericht26.txt"), hasNote("Depotpreis 2020"), //
                        hasAmount("EUR", 12.18), hasGrossValue("EUR", 12.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        dividend( //
                                        hasDate("2020-06-16"), hasExDate("2020-06-16"), //
                                        hasShares(271.125), //
                                        hasSource("Quartalsbericht26.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }
}
