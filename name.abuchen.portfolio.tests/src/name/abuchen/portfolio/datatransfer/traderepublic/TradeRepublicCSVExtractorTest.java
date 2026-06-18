package name.abuchen.portfolio.datatransfer.traderepublic;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
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

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.TestExtractorHelper;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class TradeRepublicCSVExtractorTest
{
    @Test
    public void testTransactionExport01() throws IOException
    {
        var client = new Client();
        var extractor = new TradeRepublicCSVExtractor(client);
        var errors = new ArrayList<Exception>();
        var filename = "TransactionExport01.csv";

        var results = TestExtractorHelper.runExtractor(extractor, client, getClass(), filename, errors);

        assertThat(errors, empty());

        // The full file has 338 data rows. Expected items:
        // BuySell: 162 BUY + 17 SELL + 3 FINAL_MATURITY(SELL) + 1 TILG(SELL)
        //   + 2 PRIVATE_MARKET_BUY = 185
        // AccountTransactions: 32 INTEREST + 13 DEPOSIT(INPAYMENT) + 12 DIVIDEND
        //   + 10 DEPOSIT(TRANSFER_INSTANT_INBOUND) + 9 DEPOSIT(CUSTOMER_INBOUND)
        //   + 8 GIFT + 7 TAXES(EARNINGS) + 9 TAX_REFUND + 5 DEPOSIT(TRANSFER_INBOUND)
        //   + 3 REMOVAL(OUTBOUND) + 2 DIVIDEND_EQUIVALENT_PAYMENT
        //   + 1 DISTRIBUTION(DIVIDEND) + 17 REMOVAL(TRANSFER_INSTANT_OUTBOUND)
        //   + 16 REMOVAL(CARD_TRANSACTION) + 2 DEPOSIT(BENEFITS_SAVEBACK)
        //   + 3 TAX_REFUND(SEC_ACCOUNT) + 3 TAXES(PRE_DETERMINED_TAX_BASE)
        //   + 1 FEES(SELL split) + 1 failure (TransactionItem with failure message) = 151
        // Skipped: 2 REDEMPTION + 1 WARRANT_EXERCISE + 1 FINAL_MATURITY(CA) = 4
        // Failures: 1 SPLIT = 1 (included in account tx count)
        // Securities: 39

        assertThat(countSecurities(results), is(39L));
        assertThat(countBuySell(results), is(185L));
        assertThat(countAccountTransactions(results), is(151L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(4L));
        assertThat(results.size(), is(39 + 185 + 151 + 4));

        new AssertImportActions().check(results, "EUR");

        // Deutsche Telekom BUY 2021-08-06: 15 shares @ 17.664, amount=-264.96, fee=-1.00
        // PP amount = abs(-264.96 + -1.00) = 265.96
        // grossValue = 265.96 - 1.00 = 264.96
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-06"), hasShares(15.00), //
                        hasSource(filename), //
                        hasNote("BUY DE0005557508"), //
                        hasAmount("EUR", 265.96), hasGrossValue("EUR", 264.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));

        // Xiaomi fractional BUY 2024-12-17: 0.3524 shares, amount=-1.30, no fee
        // PP amount = abs(-1.30) = 1.30
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-17"), hasShares(0.3524), //
                        hasSource(filename), //
                        hasNote("BUY KYG9830T1067"), //
                        hasAmount("EUR", 1.30), hasGrossValue("EUR", 1.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // Aker Carbon Capture SELL 2021-09-08: 35 shares @ 2.315, amount=81.03, fee=-1.00
        // PP amount = abs(81.03 + -1.00) = 80.03
        // grossValue (sell) = amount + fees = 80.03 + 1.00 = 81.03
        assertThat(results, hasItem(sale( //
                        hasDate("2021-09-08"), hasShares(35.00), //
                        hasSource(filename), //
                        hasNote("SELL NO0010890304"), //
                        hasAmount("EUR", 80.03), hasGrossValue("EUR", 81.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));

        // Bitcoin BUY 2022-01-24: 0.0011 BTC, amount=-32.97, fee=-1.00
        // PP amount = abs(-32.97 + -1.00) = 33.97
        // grossValue = 33.97 - 1.00 = 32.97
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-24"), hasShares(0.0011), //
                        hasSource(filename), //
                        hasNote("BUY BTC"), //
                        hasAmount("EUR", 33.97), hasGrossValue("EUR", 32.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));

        // EOS SELL 2025-06-12: 2.1535, amount=1.07, no fee, no tax
        // PP amount = abs(1.07) = 1.07
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-12"), hasShares(2.1535), //
                        hasSource(filename), //
                        hasNote("SELL EOS"), //
                        hasAmount("EUR", 1.07), hasGrossValue("EUR", 1.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // MSCI Europe EUR (Acc) BUY 2023-06-02: 0.6188880000 shares, amount=-100.00, no fee
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-02"), hasShares(0.618888), //
                        hasSource(filename), //
                        hasNote("BUY FR0010261198"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // Apr. 2024 bond BUY 2023-12-15: 20 shares @ 0.9895, amount=-19.79, fee=-1.00
        // PP amount = abs(-19.79 + -1.00) = 20.79
        // grossValue = 20.79 - 1.00 = 19.79
        assertThat(results, hasItem(security( //
                        hasIsin("DE0001141794"), hasWkn(null), hasTicker(null), //
                        hasName("Apr. 2024"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-15"), hasShares(20.00), //
                        hasSource(filename), //
                        hasNote("BUY DE0001141794"), //
                        hasAmount("EUR", 20.79), hasGrossValue("EUR", 19.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));

        // Long 110,2935 $ BUY 2024-05-24: 20 shares @ 3.44, amount=-68.80, fee=-1.00
        // PP amount = abs(-68.80 + -1.00) = 69.80
        // grossValue = 69.80 - 1.00 = 68.80
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-24"), hasShares(20.00), //
                        hasSource(filename), //
                        hasNote("BUY DE000TT2G3G7"), //
                        hasAmount("EUR", 69.80), hasGrossValue("EUR", 68.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));

        // Deutsche Telekom DIVIDEND 2022-04-12: 19 shares, amount=12.16, no tax
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-04-12"), hasShares(19.00), //
                        hasSource(filename), //
                        hasAmount("EUR", 12.16), hasGrossValue("EUR", 12.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // Hitachi DIVIDEND 2024-11-27: 1 share, amount=0.11 EUR, tax=-0.01
        // original_amount=17.72 JPY, fx_rate=0.006206
        // PP amount = abs(0.11 + -0.01) = 0.10
        // Security was first created via BUY (no original_currency) -> EUR
        // Security currency EUR == account currency EUR -> no FX handling
        assertThat(results, hasItem(security( //
                        hasIsin("JP3788600009"), hasWkn(null), hasTicker(null), //
                        hasName("Hitachi"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2024-11-27"), hasShares(1.00), //
                        hasSource(filename), //
                        hasNote("Cash Dividend for ISIN JP3788600009"), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.11), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));

        // Aker Carbon Capture DIVIDEND 2025-03-19: 232 shares, amount=41.58, tax=-15.53
        // PP amount = abs(41.58 + -15.53) = 26.05
        // Security was first created via BUY (no original_currency) -> EUR
        // Security currency EUR == account currency EUR -> no FX handling
        assertThat(results, hasItem(security( //
                        hasIsin("NO0010890304"), hasWkn(null), hasTicker(null), //
                        hasName("Aker Carbon Capture"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-19"), hasShares(232.00), //
                        hasSource(filename), //
                        hasNote("Cash Dividend for ISIN NO0010890304"), //
                        hasAmount("EUR", 26.05), hasGrossValue("EUR", 41.58), //
                        hasTaxes("EUR", 15.53), hasFees("EUR", 0.00))));

        // S&P 500 EUR (Dist) DISTRIBUTION 2023-12-15: 24.9188 shares, amount=11.46, tax=-2.25
        // PP amount = abs(11.46 + -2.25) = 9.21
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-15"), hasShares(24.9188), //
                        hasSource(filename), //
                        hasAmount("EUR", 9.21), hasGrossValue("EUR", 11.46), //
                        hasTaxes("EUR", 2.25), hasFees("EUR", 0.00))));

        // Aker Carbon Capture DIVIDEND_EQUIVALENT_PAYMENT 2025-03-19: 232 shares
        // amount=41.56, tax=-11.63
        // PP amount = abs(41.56 + -11.63) = 29.93
        // Security currency EUR == account currency -> no FX handling
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-19"), hasShares(232.00), //
                        hasSource(filename), //
                        hasNote("Capital Distribution for ISIN NO0010890304"), //
                        hasAmount("EUR", 29.93), hasGrossValue("EUR", 41.56), //
                        hasTaxes("EUR", 11.63), hasFees("EUR", 0.00))));

        // INTEREST_PAYMENT 2023-12-01: amount=4.77, tax=-1.34
        // PP amount = abs(4.77 + -1.34) = 3.43
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-01"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Interest payment Booking"), //
                        hasAmount("EUR", 3.43), hasGrossValue("EUR", 4.77), //
                        hasTaxes("EUR", 1.34), hasFees("EUR", 0.00))));

        // Mai 2024 bond INTEREST_PAYMENT 2024-05-15: 20.04 shares, amount=0.28, tax=-0.07
        // Bond interest with security -> DIVIDENDS (INTEREST cannot have security)
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-05-15"), hasShares(20.04), //
                        hasSource(filename), //
                        hasAmount("EUR", 0.21), hasGrossValue("EUR", 0.28), //
                        hasTaxes("EUR", 0.07), hasFees("EUR", 0.00))));

        // CUSTOMER_INBOUND 2023-05-30: amount=320.00
        assertThat(results, hasItem(deposit( //
                        hasDate("2023-05-30"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("No SepaDescription provided"), //
                        hasAmount("EUR", 320.00), hasGrossValue("EUR", 320.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // CUSTOMER_INPAYMENT 2021-08-06: amount=352.45, fee=-2.45
        // PP amount = abs(352.45 + -2.45) = 350.00
        assertThat(results, hasItem(deposit( //
                        hasDate("2021-08-06"), hasShares(0.00), //
                        hasSource(filename), //
                        hasAmount("EUR", 350.00), hasGrossValue("EUR", 352.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.45))));

        // TRANSFER_INBOUND 2024-12-16: amount=300.00
        assertThat(results, hasItem(deposit( //
                        hasDate("2024-12-16"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Incoming transfer"), //
                        hasAmount("EUR", 300.00), hasGrossValue("EUR", 300.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // TRANSFER_INSTANT_INBOUND 2025-01-20: amount=600.00
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-01-20"), hasShares(0.00), //
                        hasSource(filename), //
                        hasAmount("EUR", 600.00), hasGrossValue("EUR", 600.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // CUSTOMER_OUTBOUND_REQUEST 2024-10-22: amount=-200.00
        // PP amount = abs(-200.00) = 200.00
        assertThat(results, hasItem(removal( //
                        hasDate("2024-10-22"), hasShares(0.00), //
                        hasSource(filename), //
                        hasAmount("EUR", 200.00), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // CASH/FINAL_MATURITY for Apr. 2024 bond 2024-04-05: 20.21 shares, amount=20.21
        // PP amount = abs(20.21) = 20.21 (SELL)
        assertThat(results, hasItem(sale( //
                        hasDate("2024-04-05"), hasShares(20.21), //
                        hasSource(filename), //
                        hasAmount("EUR", 20.21), hasGrossValue("EUR", 20.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // CASH/TILG for Short 17.879 Pt. 2024-08-28: 200 shares, amount=0.20
        // PP amount = abs(0.20) = 0.20 (SELL)
        assertThat(results, hasItem(sale( //
                        hasDate("2024-08-28"), hasShares(200.00), //
                        hasSource(filename), //
                        hasNote("Warrant Exercise for ISIN DE000HS88LK3"), //
                        hasAmount("EUR", 0.20), hasGrossValue("EUR", 0.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // EARNINGS 2025-01-28 for S&P 500: amount=0, tax=-0.08
        // total = 0 + (-0.08) = -0.08 < 0 -> TAXES
        // PP amount = abs(-0.08) = 0.08
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-28"), hasShares(60.549637), //
                        hasSource(filename), //
                        hasNote("Vorabpauschale for ISIN LU0496786574"), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // TAX_OPTIMIZATION 2024-08-28: amount=0, tax=15.33
        // PP amount = abs(15.33) = 15.33
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-08-28"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Tax Optimisation"), //
                        hasAmount("EUR", 15.33), hasGrossValue("EUR", 15.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // GIFT 2024-12-05: amount=-51.00 -> REMOVAL
        assertThat(results, hasItem(removal( //
                        hasDate("2024-12-05"), hasShares(0.00), //
                        hasSource(filename), //
                        hasAmount("EUR", 51.00), hasGrossValue("EUR", 51.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // GIFT 2024-12-17: amount=26.00 -> DEPOSIT
        assertThat(results, hasItem(deposit( //
                        hasDate("2024-12-17"), hasShares(0.00), //
                        hasSource(filename), //
                        hasAmount("EUR", 26.00), hasGrossValue("EUR", 26.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // TRANSFER_INSTANT_OUTBOUND 2025-07-07: amount=-300.00, no security -> REMOVAL
        assertThat(results, hasItem(removal( //
                        hasDate("2025-07-07"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Outgoing transfer"), //
                        hasAmount("EUR", 300.00), hasGrossValue("EUR", 300.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // BENEFITS_SAVEBACK 2026-04-01: amount=4.51, no security -> DEPOSIT
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-04-01"), hasShares(0.00), //
                        hasSource(filename), //
                        hasAmount("EUR", 4.51), hasGrossValue("EUR", 4.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // PRIVATE_MARKET_BUY 2026-03-02: 100 shares, amount=-100.00, no fee
        // PP amount = abs(-100.00) = 100.00
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-03-02"), hasShares(100.00), //
                        hasSource(filename), //
                        hasNote("Private Markets pre-payment for buy order: b097723c-0096-422b-b708-6aad9ca5c210"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // Ferrari DIVIDEND 2025-05-06: 0.0601 shares, amount=0.11, tax=-0.01
        // PP amount = abs(0.11 + -0.01) = 0.10
        // Security was first created via BUY (no original_currency) -> EUR
        // Security currency EUR == account currency -> no FX handling
        assertThat(results, hasItem(security( //
                        hasIsin("NL0011585146"), hasWkn(null), hasTicker(null), //
                        hasName("Ferrari"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-06"), hasShares(0.0601), //
                        hasSource(filename), //
                        hasNote("Cash Dividend for ISIN NL0011585146"), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.11), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));

        // Netflix CORPORATE_ACTION SPLIT 2025-11-17: marked as failure
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        deposit( //
                                        hasDate("2025-11-17"), //
                                        hasSource(filename), //
                                        hasNote("SPLIT US64110L1061"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testTransactionReport02() throws IOException
    {
        var client = new Client();
        var extractor = new TradeRepublicCSVExtractor(client);
        var errors = new ArrayList<Exception>();
        var filename = "TransactionExport02.csv";

        var results = TestExtractorHelper.runExtractor(extractor, client, getClass(), filename, errors);

        assertThat(errors, empty());

        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2 + 3 + 6));

        new AssertImportActions().check(results, "EUR");

        // check Apple security
        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("Apple"), //
                        hasCurrencyCode("EUR"))));

        // check Bitcoin security (CRYPTO asset_class -> ticker only)
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                        hasName("Bitcoin"), //
                        hasCurrencyCode("EUR"))));

        // Apple BUY 2026-03-06T10:20:05.865Z: 0.223214 shares, amount=-5.69, fee=-1.00
        // PP amount = abs(-5.69 + -1.00) = 6.69
        // grossValue (buy) = amount - fees = 6.69 - 1.00 = 5.69
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-03-06T10:20:05.865"), hasShares(0.223214), //
                        hasSource(filename), //
                        hasNote("Buy trade US0378331005 APPLE INC., quantity: 0.223214"), //
                        hasAmount("EUR", 6.69), hasGrossValue("EUR", 5.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));

        // Apple SELL 2026-03-06T10:38:28.793Z: 0.223214 shares, amount=5.58, fee=-1.00
        // PP amount = abs(5.58 + -1.00) = 4.58
        // grossValue (sell) = amount + fees = 4.58 + 1.00 = 5.58
        assertThat(results, hasItem(sale( //
                        hasDate("2026-03-06T10:38:28.793"), hasShares(0.223214), //
                        hasSource(filename), //
                        hasNote("Sell trade US0378331005 APPLE INC., quantity: 0.223214"), //
                        hasAmount("EUR", 4.58), hasGrossValue("EUR", 5.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00))));

        // Bitcoin BUY 2026-04-02T11:11:12.830Z: 0.000849 shares, amount=-49.97, no fee
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-04-02T11:11:12.830"), hasShares(0.000849), //
                        hasSource(filename), //
                        hasNote("Savings plan execution XF000BTC0017 Bitcoin, quantity: 0.000849"), //
                        hasAmount("EUR", 49.97), hasGrossValue("EUR", 49.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // TRANSFER_OUT 2026-03-06T10:24:44.069415Z: amount=-53.00 -> REMOVAL
        assertThat(results, hasItem(removal( //
                        hasDate("2026-03-06T10:24:44.069415"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Outgoing transfer for Test Account (DE56449845308660297184)"), //
                        hasAmount("EUR", 53.00), hasGrossValue("EUR", 53.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // TRANSFER_OUT 2026-03-06T10:49:34.904538Z: amount=-1.00 -> REMOVAL
        assertThat(results, hasItem(removal( //
                        hasDate("2026-03-06T10:49:34.904538"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Outgoing transfer for Test Account (DE56449845308660297184)"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // CUSTOMER_INPAYMENT 2026-03-06T10:19:47.982485Z: amount=10.10, fee=-0.10
        // PP amount = abs(10.10 + -0.10) = 10.00
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-03-06T10:19:47.982485"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Card Top up with ****1234"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.10))));

        // CUSTOMER_INPAYMENT 2026-03-06T10:17:57.448665Z: amount=50.50, fee=-0.50
        // PP amount = abs(50.50 + -0.50) = 50.00
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-03-06T10:17:57.448665"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Card Top up with ****1234"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // CUSTOMER_INPAYMENT 2026-03-13T10:06:02.926905Z: amount=10.10, fee=-0.10
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-03-13T10:06:02.926905"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Card Top up with ****1234"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.10))));

        // CUSTOMER_INPAYMENT 2026-03-13T00:00:00Z: amount=48.48, fee=-0.48
        // PP amount = abs(48.48 + -0.48) = 48.00
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-03-13"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Card Top up with ****1234"), //
                        hasAmount("EUR", 48.00), hasGrossValue("EUR", 48.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.48))));
    }

    @Test
    public void testTransactionReport03() throws IOException
    {
        var client = new Client();
        var extractor = new TradeRepublicCSVExtractor(client);
        var errors = new ArrayList<Exception>();
        var filename = "TransactionExport03.csv";

        var results = TestExtractorHelper.runExtractor(extractor, client, getClass(), filename, errors);

        assertThat(errors, empty());

        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1 + 1 + 2));

        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B5BMR087"), hasWkn(null), hasTicker(null), //
                        hasName("Core S&P 500 USD (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // BUY 2026-03-23T14:03:47.702Z: 0.629770 shares, amount=-50.00, no fee
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-03-23T14:03:47.702"), hasShares(0.629770), //
                        hasSource(filename), //
                        hasNote("Savings plan execution IE00B5BMR087 iShares VII plc - iShares Core S&P 500 UCITS ETF USD (Acc), quantity: 0.629770"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // TRANSFER_IN 2026-03-06T10:49:36.950420Z: amount=1.00 -> DEPOSIT
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-03-06T10:49:36.950420"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Incoming transfer from Max Mustermann (DE06851810611671146112)"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // TRANSFER_IN 2026-03-06T10:24:46.107129Z: amount=53.00 -> DEPOSIT
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-03-06T10:24:46.107129"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Incoming transfer from Max Mustermann (DE06851810611671146112)"), //
                        hasAmount("EUR", 53.00), hasGrossValue("EUR", 53.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTransactionReport04() throws IOException
    {
        var client = new Client();
        var extractor = new TradeRepublicCSVExtractor(client);
        var errors = new ArrayList<Exception>();
        var filename = "TransactionExport04.csv";

        var results = TestExtractorHelper.runExtractor(extractor, client, getClass(), filename, errors);

        assertThat(errors, empty());

        // 8 DIVIDEND rows, 4 corrections have no matching originals in the
        // import, so each correction becomes a failure DIVIDENDS item asking
        // the user to delete any previously imported original. The 4 positive
        // rows become regular dividends.
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(4L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1 + 8));

        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US3793782018"), hasWkn(null), hasTicker(null), //
                        hasName("Global Net Lease"), //
                        hasCurrencyCode("USD"))));

        // DIVIDEND 2026-03-28T12:41:41.190983Z: 197.346557 shares, amount=32.27, no tax
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-03-28T12:41:41.190983"), hasShares(197.346557), //
                        hasSource(filename), //
                        hasNote("Cash Dividend for ISIN US3793782018"), //
                        hasAmount("EUR", 32.27), hasGrossValue("EUR", 32.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // DIVIDEND 2026-03-28T12:43:11.375393Z: 166.672527 shares, amount=27.97, tax=-5.14
        // PP amount = abs(27.97 + -5.14) = 22.83
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-03-28T12:43:11.375393"), hasShares(166.672527), //
                        hasSource(filename), //
                        hasNote("Cash Dividend for ISIN US3793782018"), //
                        hasAmount("EUR", 22.83), hasGrossValue("EUR", 27.97), //
                        hasTaxes("EUR", 5.14), hasFees("EUR", 0.00))));

        // DIVIDEND 2026-03-28T12:44:58.343321Z: 181.638106 shares, amount=29.58, tax=-7.80
        // PP amount = abs(29.58 + -7.80) = 21.78
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-03-28T12:44:58.343321"), hasShares(181.638106), //
                        hasSource(filename), //
                        hasNote("Cash Dividend for ISIN US3793782018"), //
                        hasAmount("EUR", 21.78), hasGrossValue("EUR", 29.58), //
                        hasTaxes("EUR", 7.80), hasFees("EUR", 0.00))));

        // DIVIDEND 2026-03-28T12:46:15.537436Z: 152.443728 shares, amount=40.70, tax=-10.72
        // PP amount = abs(40.70 + -10.72) = 29.98
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-03-28T12:46:15.537436"), hasShares(152.443728), //
                        hasSource(filename), //
                        hasNote("Cash Dividend for ISIN US3793782018"), //
                        hasAmount("EUR", 29.98), hasGrossValue("EUR", 40.70), //
                        hasTaxes("EUR", 10.72), hasFees("EUR", 0.00))));

        // Unmatched correction 2026-03-28T12:17:14.638045Z: 197.346557 shares,
        // amount=-32.27, tax=8.25 -> signed net -24.02, gross -32.27.
        assertThat(results, hasItem(withFailureMessage(
                        Messages.MsgErrorCashDividendCorrectionUnmatched,
                        dividend(hasDate("2026-03-28T12:17:14.638045"), hasShares(197.346557), //
                                        hasSource(filename), //
                                        hasAmount("EUR", -24.02), hasGrossValue("EUR", -32.27), //
                                        hasTaxes("EUR", -8.25), hasFees("EUR", 0.00)))));

        // Unmatched correction 2026-03-28T12:17:08.949883Z: 181.638106 shares,
        // amount=-29.58, tax=7.56 -> signed net -22.02, gross -29.58.
        assertThat(results, hasItem(withFailureMessage(
                        Messages.MsgErrorCashDividendCorrectionUnmatched,
                        dividend(hasDate("2026-03-28T12:17:08.949883"), hasShares(181.638106), //
                                        hasSource(filename), //
                                        hasAmount("EUR", -22.02), hasGrossValue("EUR", -29.58), //
                                        hasTaxes("EUR", -7.56), hasFees("EUR", 0.00)))));

        // Unmatched correction 2026-03-28T12:17:05.567508Z: 152.443728 shares,
        // amount=-40.70, tax=6.11 -> signed net -34.59, gross -40.70.
        assertThat(results, hasItem(withFailureMessage(
                        Messages.MsgErrorCashDividendCorrectionUnmatched,
                        dividend(hasDate("2026-03-28T12:17:05.567508"), hasShares(152.443728), //
                                        hasSource(filename), //
                                        hasAmount("EUR", -34.59), hasGrossValue("EUR", -40.70), //
                                        hasTaxes("EUR", -6.11), hasFees("EUR", 0.00)))));

        // Unmatched correction 2026-03-28T12:16:22.541483Z: 166.672527 shares,
        // amount=-27.97, tax=7.14 -> signed net -20.83, gross -27.97.
        assertThat(results, hasItem(withFailureMessage(
                        Messages.MsgErrorCashDividendCorrectionUnmatched,
                        dividend(hasDate("2026-03-28T12:16:22.541483"), hasShares(166.672527), //
                                        hasSource(filename), //
                                        hasAmount("EUR", -20.83), hasGrossValue("EUR", -27.97), //
                                        hasTaxes("EUR", -7.14), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testTransactionReport05() throws IOException
    {
        var client = new Client();
        var extractor = new TradeRepublicCSVExtractor(client);
        var errors = new ArrayList<Exception>();
        var filename = "TransactionExport05.csv";

        var results = TestExtractorHelper.runExtractor(extractor, client, getClass(), filename, errors);

        assertThat(errors, empty());

        // 3 DIVIDEND rows: one original (2025-07-17), one matching correction
        // (2026-03-28 12:17:08), one replacement with updated tax
        // (2026-03-28 12:44:58). The original + correction pair is removed by
        // postProcessing; only the replacement survives.
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));

        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(security( //
                        hasIsin("US3793782018"), hasWkn(null), hasTicker(null), //
                        hasName("Global Net Lease"), //
                        hasCurrencyCode("USD"))));

        // Replacement DIVIDEND 2026-03-28 12:44:58: amount=29.58, tax=-7.80
        // net = 29.58 - 7.80 = 21.78.
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-03-28T12:44:58.343321"), hasShares(181.638106), //
                        hasSource(filename), //
                        hasNote("Cash Dividend for ISIN US3793782018"), //
                        hasAmount("EUR", 21.78), hasGrossValue("EUR", 29.58), //
                        hasTaxes("EUR", 7.80), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTransactionReport06() throws IOException
    {
        var client = new Client();
        var extractor = new TradeRepublicCSVExtractor(client);
        var errors = new ArrayList<Exception>();
        var filename = "TransactionExport06.csv";

        var results = TestExtractorHelper.runExtractor(extractor, client, getClass(), filename, errors);

        assertThat(errors, empty());

        // Single TAX_OPTIMIZATION row with negative tax -> booked as TAXES.
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));

        new AssertImportActions().check(results, "EUR");

        // TAX_OPTIMIZATION 2026-02-15: amount=0, tax=-4.21 -> TAXES
        // PP amount = abs(-4.21) = 4.21
        assertThat(results, hasItem(taxes( //
                        hasDate("2026-02-15"), hasShares(0.00), //
                        hasSource(filename), //
                        hasNote("Tax Optimisation"), //
                        hasAmount("EUR", 4.21), hasGrossValue("EUR", 4.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
