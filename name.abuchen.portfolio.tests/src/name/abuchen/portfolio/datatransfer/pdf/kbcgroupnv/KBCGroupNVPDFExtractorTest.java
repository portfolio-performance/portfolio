package name.abuchen.portfolio.datatransfer.pdf.kbcgroupnv;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
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
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.KBCGroupNVPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class KBCGroupNVPDFExtractorTest
{
    @Test
    public void testAankoop01()
    {
        KBCGroupNVPDFExtractor extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Aankoop01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0013654783"), hasWkn(null), hasTicker(null), //
                        hasName("PROSUS N.V. (AS)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-02-02T14:50:02"), hasShares(15.00), //
                        hasSource("Aankoop01.txt"), //
                        hasNote("Borderel 275825809"), //
                        hasAmount("EUR", 868.50), hasGrossValue("EUR", 858.00), //
                        hasTaxes("EUR", 3.00), hasFees("EUR", 7.50))));
    }

    @Test
    public void testMultiblerAankoopVerkoop01()
    {
        KBCGroupNVPDFExtractor extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "MultiblerAankoopVerkoop01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(6L));
        assertThat(countBuySell(results), is(6L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3F81R35"), hasWkn(null), hasTicker(null), //
                        hasName("ISHAR.III CORE EUR CORP BD UC ETF-D"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B5BMR087"), hasWkn(null), hasTicker(null), //
                        hasName("IS CO S&P500 U.ETF USD(ACC-PTG.K"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE0001141836"), hasWkn(null), hasTicker(null), //
                        hasName("GERMANY 21-26 0% 10/04 REGS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("ES0000012I08"), hasWkn(null), hasTicker(null), //
                        hasName("SPAIN 21-28 0% 31/01"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("EU000A3KWCF4"), hasWkn(null), hasTicker(null), //
                        hasName("EUROP UN 21-28 0% 04/10 REGS MTN"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("EU000A3KWCF4"), hasWkn(null), hasTicker(null), //
                        hasName("EUROP UN 21-28 0% 04/10 REGS MTN"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("EU000A3KRJQ6"), hasWkn(null), hasTicker(null), //
                        hasName("EUROPEAN UNION 21-29 0% 04/07 REGS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-09-02T10:01:45"), hasShares(1070.00), //
                        hasSource("MultiblerAankoopVerkoop01.txt"), //
                        hasNote("Borderel 040826294"), //
                        hasAmount("EUR", 125824.15), hasGrossValue("EUR", 127005.21), //
                        hasTaxes("EUR", 839.15 + 151.40), hasFees("EUR", 190.51))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-09-02T10:37:23"), hasShares(94.00), //
                        hasSource("MultiblerAankoopVerkoop01.txt"), //
                        hasNote("Borderel 040836544"), //
                        hasAmount("EUR", 50477.27), hasGrossValue("EUR", 50587.98), //
                        hasTaxes("EUR", 60.71), hasFees("EUR", 50.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-02T11:46:58"), hasShares(400.00), //
                        hasSource("MultiblerAankoopVerkoop01.txt"), //
                        hasNote("Borderel 040854432"), //
                        hasAmount("EUR", 38560.00), hasGrossValue("EUR", 38500.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 60.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-02T11:53:00"), hasShares(400.00), //
                        hasSource("MultiblerAankoopVerkoop01.txt"), //
                        hasNote("Borderel 040855313"), //
                        hasAmount("EUR", 36732.00), hasGrossValue("EUR", 36672.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 60.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-02T11:58:19"), hasShares(400.00), //
                        hasSource("MultiblerAankoopVerkoop01.txt"), //
                        hasNote("Borderel 040855874"), //
                        hasAmount("EUR", 36171.28), hasGrossValue("EUR", 36068.00), //
                        hasTaxes("EUR", 43.28), hasFees("EUR", 60.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-02T12:09:44"), hasShares(400.00), //
                        hasSource("MultiblerAankoopVerkoop01.txt"), //
                        hasNote("Borderel 040857050"), //
                        hasAmount("EUR", 35622.62), hasGrossValue("EUR", 35520.00), //
                        hasTaxes("EUR", 42.62), hasFees("EUR", 60.00))));
    }

    @Test
    public void testDividende01()
    {
        KBCGroupNVPDFExtractor extractor = new KBCGroupNVPDFExtractor(new Client());

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
                        hasIsin("IE00B3F81R35"), hasWkn(null), hasTicker(null), //
                        hasName("ISHAR.III CORE EUR CORP BD UC ETF-D"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-24T00:00"), hasShares(2065.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Borderel 003308592"), //
                        hasAmount("EUR", 2862.79), hasGrossValue("EUR", 4173.16), //
                        hasTaxes("EUR", 1251.95 + 10.14), hasFees("EUR", 48.28))));
    }

    @Test
    public void testRekeninguittreksel01()
    {
        KBCGroupNVPDFExtractor extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeninguittreksel01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-09-04"), hasAmount("EUR", 32339.70), //
                        hasSource("Rekeninguittreksel01.txt"), hasNote("Overschrijving naar klant"))));
    }

    @Test
    public void testRekeninguittreksel02()
    {
        KBCGroupNVPDFExtractor extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeninguittreksel02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-08-18"), hasAmount("EUR", 50000.00), //
                        hasSource("Rekeninguittreksel02.txt"), hasNote("Provisionering rekening klant"))));
    }

    @Test
    public void testRekeninguittreksel03()
    {
        KBCGroupNVPDFExtractor extractor = new KBCGroupNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeninguittreksel03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR", "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKM4GZ66"), hasWkn(null), hasTicker(null), //
                        hasName("ISHARES PLC CORE MSC E.M.IM UC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-06-12T11:55:21"), hasShares(2300.00), //
                        hasSource("Rekeninguittreksel03.txt"), //
                        hasNote("Borderel 017462864"), //
                        hasAmount("USD", 69606.12), hasGrossValue("USD", 69743.43), //
                        hasForexGrossValue("EUR", 64918.22), //
                        hasTaxes("USD", (78.06 / 0.932651)), hasFees("USD", (50.00 / 0.932651)))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-12"), hasAmount("EUR", 45000.00), //
                        hasSource("Rekeninguittreksel03.txt"), hasNote("Provisionering rekening klant"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-12"), hasAmount("EUR", 132673.91), //
                        hasSource("Rekeninguittreksel03.txt"), hasNote("Provisionering rekening klant"))));
    }
}
