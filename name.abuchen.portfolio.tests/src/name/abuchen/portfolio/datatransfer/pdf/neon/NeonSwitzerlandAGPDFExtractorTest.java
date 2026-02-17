package name.abuchen.portfolio.datatransfer.pdf.neon;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
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

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.NeonSwitzerlandAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class NeonSwitzerlandAGPDFExtractorTest
{
    @Test
    public void testBuy01()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "CHF");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0394628066"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Inv.II Mo.Re.Opp"))));

        // check 1st buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-24"), //
                        hasShares(0.102347), //
                        hasSource("Buy01.txt"), //
                        hasAmount("CHF", 10.00), //
                        hasGrossValue("CHF", 10.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0215804714"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Ind EqLar Cap CH"))));

        // check 2nd buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-24"), //
                        hasShares(2.657624), //
                        hasSource("Buy01.txt"), //
                        hasAmount("CHF", 480.00), //
                        hasGrossValue("CHF", 480.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 3rd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0132501898"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Eq S&Mid Caps CH"))));

        // check 3rd buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-24"), //
                        hasShares(0.351181), //
                        hasSource("Buy01.txt"), //
                        hasAmount("CHF", 120.00), //
                        hasGrossValue("CHF", 120.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));
    }

    @Test
    public void testBuy02()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "CHF");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0117044971"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc F. V Eq Emer. M."))));

        // check 1st buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-25"), //
                        hasShares(0.580442), //
                        hasSource("Buy02.txt"), //
                        hasAmount("CHF", 80.00), //
                        hasGrossValue("CHF", 80.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0117044906"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Ind Eq Wo.ex.CH"))));

        // check 2nd buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-25"), //
                        hasShares(0.932137), //
                        hasSource("Buy02.txt"), //
                        hasAmount("CHF", 310.00), //
                        hasGrossValue("CHF", 310.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));
    }

    @Test
    public void testBuy03()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "CHF");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0117044906"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Ind Eq Wo.ex.CH"))));

        // check 1st buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-16"), //
                        hasShares(5.552571), //
                        hasSource("Buy03.txt"), //
                        hasAmount("CHF", 1939.98), //
                        hasGrossValue("CHF", 1939.98), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0117044971"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc F. V Eq Emer. M."))));

        // check 2nd buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-16"), //
                        hasShares(3.308542), //
                        hasSource("Buy03.txt"), //
                        hasAmount("CHF", 500.64), //
                        hasGrossValue("CHF", 500.64), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check deposit
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-01-16"), //
                        hasSource("Buy03.txt"), //
                        hasAmount("CHF", 1000.00))));
    }

    @Test
    public void testBuy04()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "CHF");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0394628066"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Inv.II Mo.Re.Opp"))));

        // check 1st buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-26"), //
                        hasShares(0.10248), //
                        hasSource("Buy04.txt"), //
                        hasAmount("CHF", 10.00), //
                        hasGrossValue("CHF", 10.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0215804714"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Ind EqLar Cap CH"))));

        // check 2nd buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-26"), //
                        hasShares(2.519818), //
                        hasSource("Buy04.txt"), //
                        hasAmount("CHF", 480.00), //
                        hasGrossValue("CHF", 480.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 3rd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0132501898"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Eq S&Mid Caps CH"))));

        // check 3rd buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-26"), //
                        hasShares(0.323213), //
                        hasSource("Buy04.txt"), //
                        hasAmount("CHF", 120.00), //
                        hasGrossValue("CHF", 120.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));
    }

    @Test
    public void testSell01()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "CHF");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0117044906"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Ind Eq Wo.ex.CH"))));

        // check 1st sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.000439), //
                        hasSource("Sell01.txt"), //
                        hasAmount("CHF", 0.15), //
                        hasGrossValue("CHF", 0.15), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0117044971"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc F. V Eq Emer. M."))));

        // check 2nd sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.000213), //
                        hasSource("Sell01.txt"), //
                        hasAmount("CHF", 0.03), //
                        hasGrossValue("CHF", 0.03), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 3rd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0132501898"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Eq S&Mid Caps CH"))));

        // check 3rd sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.000172), //
                        hasSource("Sell01.txt"), //
                        hasAmount("CHF", 0.06), //
                        hasGrossValue("CHF", 0.06), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 4th security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0215804714"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Ind EqLar Cap CH"))));

        // check 4th sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.001239), //
                        hasSource("Sell01.txt"), //
                        hasAmount("CHF", 0.23), //
                        hasGrossValue("CHF", 0.23), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));
    }

    @Test
    public void testSell02()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "CHF");

        // check 1st sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.000439), //
                        hasSource("Sell02.txt"), //
                        hasAmount("CHF", 0.15), //
                        hasGrossValue("CHF", 0.15), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 2nd sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.000213), //
                        hasSource("Sell02.txt"), //
                        hasAmount("CHF", 0.03), //
                        hasGrossValue("CHF", 0.03), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 3rd sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.000172), //
                        hasSource("Sell02.txt"), //
                        hasAmount("CHF", 0.06), //
                        hasGrossValue("CHF", 0.06), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 4th sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.001239), //
                        hasSource("Sell02.txt"), //
                        hasAmount("CHF", 0.23), //
                        hasGrossValue("CHF", 0.23), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));
    }

    @Test
    public void testDeposit01()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Deposit01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check deposit
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-11-18"), //
                        hasSource("Deposit01.txt"), //
                        hasAmount("CHF", 1000.00))));
    }

    @Test
    public void testDeposit02()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Deposit02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check deposit
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-01-08"), //
                        hasSource("Deposit02.txt"), //
                        hasAmount("CHF", 6258.00))));
    }

    @Test
    public void testFees01()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fees01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check management fee
        assertThat(results, hasItem(fee( //
                        hasDate("2025-12-03"), //
                        hasSource("Fees01.txt"), //
                        hasNote("Assessment from 24.11.25 - 31.12.25"), //
                        hasAmount("CHF", 0.47))));
    }

    // ========== Unit Tests for ISIN Conversion Utility Methods ==========

    @Test
    public void testNormalizNsin_WithCommas()
    {
        // Swiss Valor format with delimiters: 039,462,806
        String normalized = NeonSwitzerlandAGPDFExtractor.normalizeNsin("039,462,806");
        assertThat(normalized, is("039462806"));
    }

    @Test
    public void testNormalizNsin_WithSpaces()
    {
        String normalized = NeonSwitzerlandAGPDFExtractor.normalizeNsin("039 462 806");
        assertThat(normalized, is("039462806"));
    }

    @Test
    public void testNormalizNsin_WithMixedSeparators()
    {
        String normalized = NeonSwitzerlandAGPDFExtractor.normalizeNsin("039-462.806");
        assertThat(normalized, is("039462806"));
    }

    @Test
    public void testNormalizNsin_AlreadyNormalized()
    {
        String normalized = NeonSwitzerlandAGPDFExtractor.normalizeNsin("039462806");
        assertThat(normalized, is("039462806"));
    }

    @Test
    public void testNormalizNsin_LessThan9Digits()
    {
        // Should be padded with leading zeros
        String normalized = NeonSwitzerlandAGPDFExtractor.normalizeNsin("123456");
        assertThat(normalized, is("000123456"));
    }

    @Test
    public void testNormalizNsin_IgnoresCaseAndWhitespace()
    {
        String normalized = NeonSwitzerlandAGPDFExtractor.normalizeNsin("  ABC def 123  ");
        // Should uppercase and remove whitespace, then pad to 9 chars
        assertThat(normalized, is("ABCDEF123"));
    }

    @Test
    public void testComputeCheckDigit_RealIsin1()
    {
        // CH0394628066 (Money Market Fund)
        // Body is: CH039462806 (11 chars)
        String checkDigit = NeonSwitzerlandAGPDFExtractor.computeCheckDigit("CH039462806");
        assertThat(checkDigit, is("6"));
    }

    @Test
    public void testComputeCheckDigit_RealIsin2()
    {
        // CH0215804714 (Large Caps Switzerland)
        String checkDigit = NeonSwitzerlandAGPDFExtractor.computeCheckDigit("CH021580471");
        assertThat(checkDigit, is("4"));
    }

    @Test
    public void testComputeCheckDigit_RealIsin3()
    {
        // CH0132501898 (Small & Mid Caps Switzerland)
        String checkDigit = NeonSwitzerlandAGPDFExtractor.computeCheckDigit("CH013250189");
        assertThat(checkDigit, is("8"));
    }

    @Test
    public void testComputeCheckDigit_RealIsin4()
    {
        // CH0117044906 (World ex CH)
        String checkDigit = NeonSwitzerlandAGPDFExtractor.computeCheckDigit("CH011704490");
        assertThat(checkDigit, is("6"));
    }

    @Test
    public void testComputeCheckDigit_RealIsin5()
    {
        // CH0117044971 (Emerging Markets)
        String checkDigit = NeonSwitzerlandAGPDFExtractor.computeCheckDigit("CH011704497");
        assertThat(checkDigit, is("1"));
    }

    @Test
    public void testToIsin_MoneyMarketFund()
    {
        // Valor 039,462,806 should produce CH0394628066
        String isin = NeonSwitzerlandAGPDFExtractor.toIsin("CH", "039,462,806");
        assertThat(isin, is("CH0394628066"));
    }

    @Test
    public void testToIsin_EquityWorldExCH()
    {
        // Valor 011,704,490 should produce CH0117044906
        String isin = NeonSwitzerlandAGPDFExtractor.toIsin("CH", "011,704,490");
        assertThat(isin, is("CH0117044906"));
    }

    @Test
    public void testToIsin_EquityEmergingMarkets()
    {
        // Valor 011,704,497 should produce CH0117044971
        String isin = NeonSwitzerlandAGPDFExtractor.toIsin("CH", "011,704,497");
        assertThat(isin, is("CH0117044971"));
    }

    @Test
    public void testToIsin_EquityLargeCapsSwitzerland()
    {
        // Valor 021,580,471 should produce CH0215804714
        String isin = NeonSwitzerlandAGPDFExtractor.toIsin("CH", "021,580,471");
        assertThat(isin, is("CH0215804714"));
    }

    @Test
    public void testToIsin_EquitySmallMidCapsSwitzerland()
    {
        // Valor 013,250,189 should produce CH0132501898
        String isin = NeonSwitzerlandAGPDFExtractor.toIsin("CH", "013,250,189");
        assertThat(isin, is("CH0132501898"));
    }

    @Test
    public void testToIsin_BondCorporateWorld()
    {
        // Valor 011,705,251 should produce CH0117052511
        String isin = NeonSwitzerlandAGPDFExtractor.toIsin("CH", "011,705,251");
        assertThat(isin, is("CH0117052511"));
    }

    @Test
    public void testToIsin_RealEstateSwitzerland()
    {
        // Valor 011,705,254 should produce CH0117052545
        String isin = NeonSwitzerlandAGPDFExtractor.toIsin("CH", "011,705,254");
        assertThat(isin, is("CH0117052545"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToIsin_InvalidCountryCode()
    {
        NeonSwitzerlandAGPDFExtractor.toIsin("XYZ", "012345678");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToIsin_NullCountryCode()
    {
        NeonSwitzerlandAGPDFExtractor.toIsin(null, "012345678");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizNsin_Null()
    {
        NeonSwitzerlandAGPDFExtractor.normalizeNsin(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizNsin_Empty()
    {
        NeonSwitzerlandAGPDFExtractor.normalizeNsin("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizNsin_TooLong()
    {
        NeonSwitzerlandAGPDFExtractor.normalizeNsin("1234567890123");
    }
}
