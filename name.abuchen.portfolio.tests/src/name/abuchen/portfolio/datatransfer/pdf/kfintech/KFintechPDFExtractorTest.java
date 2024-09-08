package name.abuchen.portfolio.datatransfer.pdf.kfintech;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
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

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.KFintechPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class KFintechPDFExtractorTest
{
    @Test
    public void testConsolidatedAccountStatement01()
    {
        KFintechPDFExtractor extractor = new KFintechPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor
                        .extract(PDFInputFile.loadTestCase(getClass(), "consolidated_account_statement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(30L));
        assertThat(countBuySell(results), is(562L));
        assertThat(countAccountTransactions(results), is(94L));
        assertThat(results.size(), is(686));
        new AssertImportActions().check(results, "INR");

        checkAdityaBirlaSunLife(results);
        checkAXISMutualFund(results);

        // Flexi Cap Fund - Direct Plan with 34 transactions + 1 security
        assertThat(results.stream().filter(
                        item -> "INF740K01PI2".equals(item.getSecurity() != null ? item.getSecurity().getIsin() : ""))
                        .count(), is(35L));

        checkDSPFlexiCapFundRegularPlan(results);
        checkFranklinIndiaShortTermIncomePlan(results);
        checkFranklinIndiaShortTermIncomePlanRetailPlan(results);
        checkHDFCCapitalBuilderValueFund(results);
        checkHDFCFocused30Fund(results);
        checkSBIBlueChipFund(results);
    }

    private void checkAdityaBirlaSunLife(List<Item> results)
    {
        assertThat(results, hasItem(security( //
                        hasIsin("INF084M01DK3"),
                        hasName("Aditya Birla Sun Life Balanced Advantage Fund Direct Plan IDCW# Payout Option (Non-Demat)"), //
                        hasCurrencyCode("INR"))));

        var security = results.stream().filter(SecurityItem.class::isInstance)
                        .filter(item -> "INF084M01DK3".equals(item.getSecurity().getIsin())).findFirst().get()
                        .getSecurity();

        var transactions = results.stream().filter(item -> item.getSecurity() == security).toList();

        // on the PDF: 86 transactions (purchase + payout) + 1 security item
        assertThat(transactions.stream().count(), is(87L));

        // check that taxes are folded into dividend transaction
        assertThat(transactions, hasItem(dividend( //
                        hasDate("2020-04-24"), hasShares(0), //
                        hasSource("consolidated_account_statement01.txt"), //
                        hasNote(null), //
                        hasAmount("INR", 855.73 - 95.00), hasGrossValue("INR", 855.73), //
                        hasTaxes("INR", 95.00), hasFees("INR", 0.00))));

        // check that only taxes on the next line are included
        assertThat(transactions, hasItem(dividend( //
                        hasDate("2020-02-25"), hasShares(0), //
                        hasSource("consolidated_account_statement01.txt"), //
                        hasNote(null), //
                        hasAmount("INR", 1147.89), hasGrossValue("INR", 1147.89), //
                        hasTaxes("INR", 0), hasFees("INR", 0.00))));

        // check that taxes are folded into the dividend transaction across the
        // page break
        assertThat(transactions, hasItem(dividend( //
                        hasDate("2023-01-25"), hasShares(0), //
                        hasSource("consolidated_account_statement01.txt"), //
                        hasNote(null), //
                        hasAmount("INR", 980.02 - 109.00), hasGrossValue("INR", 980.02), //
                        hasTaxes("INR", 109.00), hasFees("INR", 0.00))));
    }

    private void checkAXISMutualFund(List<Item> results)
    {
        assertThat(results, hasItem(security( //
                        hasIsin("INF846K01Y05"), //
                        hasName("Axis Innovation Fund - Regular Growth"), //
                        hasCurrencyCode("INR"))));

        var security = results.stream().filter(SecurityItem.class::isInstance)
                        .filter(item -> "INF846K01Y05".equals(item.getSecurity().getIsin())).findFirst().get()
                        .getSecurity();

        var transactions = results.stream().filter(item -> item.getSecurity() == security).toList();

        // on the PDF: 1 purchase + 1 security item
        assertThat(transactions.stream().count(), is(2L));

        assertThat(transactions, hasItem(purchase( //
                        hasDate("2020-12-24"), hasShares(9999.500), //
                        hasSource("consolidated_account_statement01.txt"), //
                        hasNote(null), //
                        hasAmount("INR", 99995.00 + 5.00), hasGrossValue("INR", 99995.00), //
                        hasTaxes("INR", 5.00), hasFees("INR", 0))));
    }

    private void checkDSPFlexiCapFundRegularPlan(List<Item> results)
    {
        assertThat(results, hasItem(security( //
                        hasIsin("INF740K01037"), //
                        hasName("DSP Flexi Cap Fund - Regular Plan - Growth (Non-Demat)"), //
                        hasCurrencyCode("INR"))));

        var security = results.stream().filter(SecurityItem.class::isInstance)
                        .filter(item -> "INF740K01037".equals(item.getSecurity().getIsin())).findFirst().get()
                        .getSecurity();

        var transactions = results.stream().filter(item -> item.getSecurity() == security).toList();

        // on the PDF: 145 purchase + 1 security item
        assertThat(transactions.stream().count(), is(146L));

        // check that stamp duty on next page is included
        assertThat(transactions, hasItem(purchase( //
                        hasDate("2023-09-07"), hasShares(26.056), //
                        hasSource("consolidated_account_statement01.txt"), //
                        hasNote(null), //
                        hasAmount("INR", 2000.00), hasGrossValue("INR", 1999.90), //
                        hasTaxes("INR", 0.10), hasFees("INR", 0))));
    }

    private void checkFranklinIndiaShortTermIncomePlan(List<Item> results)
    {
        assertThat(results, hasItem(security( //
                        hasIsin("INF090I01304"), //
                        hasName("Franklin India Short Term Income Plan - Retail Plan - Growth (Non-Demat)"), //
                        hasCurrencyCode("INR"))));

        var security = results.stream().filter(SecurityItem.class::isInstance)
                        .filter(item -> "INF090I01304".equals(item.getSecurity().getIsin())).findFirst().get()
                        .getSecurity();

        var transactions = results.stream().filter(item -> item.getSecurity() == security).toList();

        // on the PDF: 1 purchase + 1 sale + 1 security item
        assertThat(transactions.stream().count(), is(3L));

        // check that stamp duty on next page is included
        assertThat(transactions, hasItem(sale( //
                        hasDate("2020-02-10"), hasShares(49.988), //
                        hasSource("consolidated_account_statement01.txt"), //
                        hasNote("Redemption Of Units"), //
                        hasAmount("INR", 197513.41), hasGrossValue("INR", 197513.41), //
                        hasTaxes("INR", 0), hasFees("INR", 0))));
    }

    private void checkFranklinIndiaShortTermIncomePlanRetailPlan(List<Item> results)
    {
        assertThat(results, hasItem(security( //
                        hasIsin("INF090I01UT3"), //
                        hasName("Franklin India Short term Income Plan- Retail Plan-Segrd PF 2(10.90% Vodafone Idea Ltd 02Sep2023 GR) (Non-Demat)"), //
                        hasCurrencyCode("INR"))));

        var security = results.stream().filter(SecurityItem.class::isInstance)
                        .filter(item -> "INF090I01UT3".equals(item.getSecurity().getIsin())).findFirst().get()
                        .getSecurity();

        var transactions = results.stream().filter(item -> item.getSecurity() == security).toList();

        // on the PDF: 1 creation + 4 sale + 1 security item
        assertThat(transactions.stream().count(), is(6L));

        // check creation of units
        assertThat(transactions, hasItem(purchase( //
                        hasDate("2020-01-24"), hasShares(49.988), //
                        hasSource("consolidated_account_statement01.txt"), //
                        hasNote("Creation of units - Segregated Portfolio- 11/04/2019"), //
                        hasAmount("INR", 1.00), hasGrossValue("INR", 1.00), //
                        hasTaxes("INR", 0), hasFees("INR", 0))));
    }

    private void checkHDFCCapitalBuilderValueFund(List<Item> results)
    {
        assertThat(results, hasItem(security( //
                        hasIsin("INF179K01VC4"), //
                        hasName("HDFC Capital Builder Value Fund - Direct Plan - Growth Option (formerly HDFC Capital Builder Fund) (Non-Demat)"), //
                        hasCurrencyCode("INR"))));

        var security = results.stream().filter(SecurityItem.class::isInstance)
                        .filter(item -> "INF179K01VC4".equals(item.getSecurity().getIsin())).findFirst().get()
                        .getSecurity();

        var transactions = results.stream().filter(item -> item.getSecurity() == security).toList();

        // on the PDF: 1 switch-in + 1 switch-out + 1 security item
        assertThat(transactions.stream().count(), is(3L));

        // switch in
        assertThat(transactions, hasItem(purchase( //
                        hasDate("2018-01-29"), hasShares(175.644), //
                        hasSource("consolidated_account_statement01.txt"), //
                        hasNote("Switch-In - From HDFC Arbitrage Fund-WS-Growth - via myCAMS Mobile"), //
                        hasAmount("INR", 56831.95), hasGrossValue("INR", 56831.95), //
                        hasTaxes("INR", 0), hasFees("INR", 0))));

        // switch out
        assertThat(transactions, hasItem(sale( //
                        hasDate("2018-04-30"), hasShares(175.644), //
                        hasSource("consolidated_account_statement01.txt"), //
                        hasNote("Switch-Out - To HDFC Small Cap Fund - Direct Growth Plan - via myCAMS"), //
                        hasAmount("INR", 54941.15 - 1.00), hasGrossValue("INR", 54941.15), //
                        hasTaxes("INR", 0), hasFees("INR", 1))));
    }

    private void checkHDFCFocused30Fund(List<Item> results)
    {
        assertThat(results, hasItem(security( //
                        hasIsin("INF179K01574"), //
                        hasName("HDFC Focused 30 Fund - Regular Plan - Growth (formerly HDFC Core and Satellite Fund) (Non-Demat)"), //
                        hasCurrencyCode("INR"))));

        var security = results.stream().filter(SecurityItem.class::isInstance)
                        .filter(item -> "INF179K01574".equals(item.getSecurity().getIsin())).findFirst().get()
                        .getSecurity();

        var transactions = results.stream().filter(item -> item.getSecurity() == security).toList();

        // on the PDF: 32 transactions + 1 security item
        assertThat(transactions.stream().count(), is(33L));

        // switch in with stamp duty
        assertThat(transactions, hasItem(purchase( //
                        hasDate("2022-11-02"), hasShares(75.446), //
                        hasSource("consolidated_account_statement01.txt"), //
                        hasNote("Switch-In - From HDFC Ultra Short Term Fund - Reg Gr Instalment No -"), //
                        hasAmount("INR", 10000), hasGrossValue("INR", 9999.50), //
                        hasTaxes("INR", 0.50), hasFees("INR", 0))));

        // purchase
        assertThat(transactions, hasItem(purchase( //
                        hasDate("2023-09-06"), hasShares(194.750), //
                        hasSource("consolidated_account_statement01.txt"), //
                        hasNote(null), //
                        hasAmount("INR", 30000), hasGrossValue("INR", 29998.50), //
                        hasTaxes("INR", 1.50), hasFees("INR", 0))));
    }

    private void checkSBIBlueChipFund(List<Item> results)
    {
        assertThat(results, hasItem(security( //
                        hasIsin("INF200K01QX4"), //
                        hasName("SBI Blue Chip Fund - Direct Plan - Growth (Non-Demat)"), //
                        hasCurrencyCode("INR"))));

        var security = results.stream().filter(SecurityItem.class::isInstance)
                        .filter(item -> "INF200K01QX4".equals(item.getSecurity().getIsin())).findFirst().get()
                        .getSecurity();

        var transactions = results.stream().filter(item -> item.getSecurity() == security).toList();

        // on the PDF: 1 switch in + 1 security item
        assertThat(transactions.stream().count(), is(2L));

        // switch in
        assertThat(transactions, hasItem(purchase( //
                        hasDate("2017-02-27"), hasShares(200.438), //
                        hasSource("consolidated_account_statement01.txt"), //
                        hasNote("Switch In - From SBI Contra Fund- Regular Plan Div"), //
                        hasAmount("INR", 6718.50), hasGrossValue("INR", 6718.50), //
                        hasTaxes("INR", 0), hasFees("INR", 0))));
    }

}
