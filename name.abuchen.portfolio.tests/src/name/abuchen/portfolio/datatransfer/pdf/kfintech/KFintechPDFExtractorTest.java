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

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
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
        var extractor = new KFintechPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "consolidated_account_statement01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(30L));
        assertThat(countBuySell(results), is(562L));
        assertThat(countAccountTransactions(results), is(94L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
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

    @Test
    public void testConsolidatedAccountStatement02()
    {
        var extractor = new KFintechPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "consolidated_account_statement02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(11L));
        assertThat(countBuySell(results), is(476L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(487));
        new AssertImportActions().check(results, "INR");

        // Mirae Asset Aggressive Hybrid Fund

        assertThat(results, hasItem(security( //
                        hasIsin("INF769K01DE6"), //
                        hasName("Mirae Asset Aggressive Hybrid Fund (formerly Mirae Asset Hybrid-Equity Fund ) - Regular Plan"), //
                        hasCurrencyCode("INR"))));

        var transactions = results.stream().filter(
                        item -> item.getSecurity() != null && "INF769K01DE6".equals(item.getSecurity().getIsin()))
                        .toList();

        assertThat(transactions, hasItem(purchase( //
                        hasDate("2018-08-23"), hasShares(6967.67), //
                        hasAmount("INR", 100000), hasGrossValue("INR", 100000), //
                        hasTaxes("INR", 0), hasFees("INR", 0))));

        assertThat(transactions, hasItem(purchase( //
                        hasDate("2018-08-29"), hasShares(415.139), //
                        hasAmount("INR", 6000), hasGrossValue("INR", 6000), //
                        hasTaxes("INR", 0), hasFees("INR", 0))));

        assertThat(transactions, hasItem(purchase( //
                        hasDate("2018-10-24"), hasShares(453.378), //
                        hasAmount("INR", 6000), hasGrossValue("INR", 6000), //
                        hasTaxes("INR", 0), hasFees("INR", 0))));

        // NIPPON INDIA RETIREMENT FUND

        assertThat(results, hasItem(security( //
                        hasIsin("INF204KA1B64"), //
                        hasName("NIPPON INDIA RETIREMENT FUND - WEALTH CREATION SCHEME - GROWTH PLAN"), //
                        hasCurrencyCode("INR"))));

        transactions = results.stream().filter(
                        item -> item.getSecurity() != null && "INF204KA1B64".equals(item.getSecurity().getIsin()))
                        .toList();

        assertThat(transactions, hasItem(purchase( //
                        hasDate("2016-06-23"), hasShares(2504.659), //
                        hasAmount("INR", 25000), hasGrossValue("INR", 25000), //
                        hasTaxes("INR", 0), hasFees("INR", 0))));

    }

    @Test
    public void testConsolidatedAccountStatement03()
    {
        var extractor = new KFintechPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "consolidated_account_statement03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(10L));
        assertThat(countBuySell(results), is(764L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(774));
        new AssertImportActions().check(results, "INR");

        // testing the three cases mentioned here:
        // https://forum.portfolio-performance.info/t/import-pdf-kfintech-india/32633/12

        // A. Scheme Name : Mirae Large and Midcap Fund
        // ISIN : INF769K01101
        // Line No at which Scheme Appears : 498
        // Transactions failed to be imported between Line Numbers : 502 to 626

        assertThat(results, hasItem(security( //
                        hasIsin("INF769K01101"), //
                        hasName("Mirae Asset Large and Midcap Fund (formerly Mirae Asset Emerging Bluechip Fund) - Regular Plan"), //
                        hasCurrencyCode("INR"))));

        var transactions = results.stream() //
                        .filter(item -> item.getSecurity() != null
                                        && "INF769K01101".equals(item.getSecurity().getIsin()))
                        .filter(BuySellEntryItem.class::isInstance) //
                        .toList();

        assertThat(transactions.size(), is(54));

        assertThat(transactions, hasItem(purchase( //
                        hasDate("2020-11-25"), hasShares(99.365), //
                        hasAmount("INR", 6500.00), hasGrossValue("INR", 6499.68), //
                        hasTaxes("INR", 0.32), hasFees("INR", 0))));

        assertThat(transactions, hasItem(purchase( //
                        hasDate("2020-12-28"), hasShares(92.792), //
                        hasAmount("INR", 6500.00), hasGrossValue("INR", 6499.68), //
                        hasTaxes("INR", 0.32), hasFees("INR", 0))));

        // B. Scheme Name : Nippon India Multicap
        // ISIN : INF204K01489
        // Line no at which scheme appears : 645
        // Transaction imported only at : Line No 649
        // Transaction did not import : from Line no 650 to 893 (purchase)

        assertThat(results, hasItem(security( //
                        hasIsin("INF204K01489"), //
                        hasName("NIPPON INDIA MULTI CAP FUND - GROWTH PLAN GROWTH OPTION"), //
                        hasCurrencyCode("INR"))));

        transactions = results.stream() //
                        .filter(item -> item.getSecurity() != null
                                        && "INF204K01489".equals(item.getSecurity().getIsin()))
                        .filter(BuySellEntryItem.class::isInstance) //
                        .toList();

        assertThat(transactions.size(), is(161));

        assertThat(transactions, hasItem(purchase( //
                        hasDate("2012-02-02"), hasShares(28.883), //
                        hasAmount("INR", 1000.00), hasGrossValue("INR", 1000.00), //
                        hasTaxes("INR", 0), hasFees("INR", 0))));

        // C. Scheme Name : Nippon India Pharma
        // ISIN : INF204KO1968
        // Scheme appears at : Line 1087
        // Transaction Imported : only Line 1091
        // Transaction missed to be imported : from Line 1092 to 1334 (purchase)

        assertThat(results, hasItem(security( //
                        hasIsin("INF204K01968"), //
                        hasName("NIPPON INDIA PHARMA FUND - GROWTH PLAN"), //
                        hasCurrencyCode("INR"))));

        transactions = results.stream() //
                        .filter(item -> item.getSecurity() != null
                                        && "INF204K01968".equals(item.getSecurity().getIsin()))
                        .filter(BuySellEntryItem.class::isInstance) //
                        .toList();

        assertThat(transactions.size(), is(161));

        assertThat(transactions, hasItem(purchase( //
                        hasDate("2025-04-21"), hasShares(2.030), //
                        hasAmount("INR", 1000.00), hasGrossValue("INR", 999.95), //
                        hasTaxes("INR", 0.05), hasFees("INR", 0))));
    }

}
