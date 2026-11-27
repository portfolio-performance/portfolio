package name.abuchen.portfolio.snapshot.advancelumpsum;

import static name.abuchen.portfolio.junit.PortfolioBuilder.amountOf;
import static name.abuchen.portfolio.junit.PortfolioBuilder.sharesOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.advancelumpsum.AdvanceLumpSumCalculation.Result;
import name.abuchen.portfolio.snapshot.advancelumpsum.BaseInterestRate.Source;

@SuppressWarnings("nls")
public class AdvanceLumpSumCalculationTest
{
    private static final BigDecimal TAX_RATE = new BigDecimal("0.26375");
    private static final LocalDate END_OF_2023 = LocalDate.parse("2023-12-31");

    private Client client;
    private Account account;

    @Before
    public void setUp()
    {
        var defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.GERMANY);
            client = new Client();
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }
        account = new AccountBuilder().addTo(client);
    }

    private Security fund(double partialExemption, long priceEnd)
    {
        var security = new SecurityBuilder() //
                        .addPrice("2022-12-30", Values.Quote.factorize(100)) //
                        .addPrice("2023-01-02", Values.Quote.factorize(100)) //
                        .addPrice("2023-12-29", Values.Quote.factorize(priceEnd)) //
                        .addTo(client);
        var attribute = client.getSettings().getOptionalPartialExemptionAttributeType().orElseThrow();
        security.getAttributes().put(attribute, partialExemption);
        return security;
    }

    private Result calculate(int year, String rate, LocalDate valuationDate)
    {
        return calculate(year, rate, valuationDate, Map.of());
    }

    private Result calculate(int year, String rate, LocalDate valuationDate, Map<Portfolio, Long> exemptionOrders)
    {
        return calculate(year, rate, valuationDate, Map.of(), exemptionOrders);
    }

    private Result calculate(int year, String rate, LocalDate valuationDate, Map<Portfolio, Long> lossBalances,
                    Map<Portfolio, Long> exemptionOrders)
    {
        var baseInterestRate = new BaseInterestRate(year, new BigDecimal(rate), Source.MANUAL);
        return new AdvanceLumpSumCalculation(client, new TestCurrencyConverter()).calculate(baseInterestRate,
                        TAX_RATE, valuationDate, lossBalances, exemptionOrders);
    }

    private Result calculate2023()
    {
        return calculate(2023, "2.55", END_OF_2023);
    }

    private static Money euro(double amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    @Test
    public void testAccumulatingFund()
    {
        var fund = fund(0.3, 110);
        new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);

        var result = calculate2023();

        assertThat(result.items(), hasSize(1));
        var item = result.items().get(0);
        assertThat(item.shares(), is(sharesOf(10)));
        assertThat(item.baseYieldPerShare().getAmount(), is(Values.Quote.factorize(1.785)));
        // 100 * 2.55% * 0.7 = 1.785 per share
        assertThat(item.advanceLumpSum(), is(euro(17.85)));
        // 30% partial exemption
        assertThat(item.taxableAmount(), is(euro(12.50)));
        assertThat(item.tax(), is(euro(3.30)));
        assertThat(item.hasMissingPrices(), is(false));
        assertThat(item.hasOutdatedPrices(), is(false));
        assertThat(item.priceStartDate(), is(LocalDate.parse("2022-12-30")));
        assertThat(item.priceEndDate(), is(LocalDate.parse("2023-12-29")));
        assertThat(result.getTotalTax(), is(euro(3.30)));
    }

    @Test
    public void testPricesFarFromTurnOfYearAreFlaggedButStillUsed()
    {
        var attribute = client.getSettings().getOptionalPartialExemptionAttributeType().orElseThrow();
        var staleStart = new SecurityBuilder().addPrice("2022-11-30", Values.Quote.factorize(100))
                        .addPrice("2023-02-01", Values.Quote.factorize(105))
                        .addPrice("2023-12-29", Values.Quote.factorize(110)).addTo(client);
        var lateFirstPrice = new SecurityBuilder().addPrice("2023-03-01", Values.Quote.factorize(100))
                        .addPrice("2023-12-29", Values.Quote.factorize(110)).addTo(client);
        var staleEnd = new SecurityBuilder().addPrice("2022-12-30", Values.Quote.factorize(100))
                        .addPrice("2023-01-02", Values.Quote.factorize(100))
                        .addPrice("2023-11-30", Values.Quote.factorize(110)).addTo(client);
        for (var fund : List.of(staleStart, lateFirstPrice, staleEnd))
        {
            fund.getAttributes().put(attribute, 0.3);
            new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);
        }

        var items = calculate2023().items();

        assertThat(items, hasSize(3));
        for (var item : items)
        {
            assertThat(item.hasOutdatedPrices(), is(true));
            assertThat(item.hasMissingPrices(), is(false));
            assertThat(item.advanceLumpSum(), is(euro(17.85)));
        }
    }

    @Test
    public void testCurrentYearEndPriceIsComparedWithValuationDate()
    {
        var fund = new SecurityBuilder().addPrice("2022-12-30", Values.Quote.factorize(100))
                        .addPrice("2023-01-02", Values.Quote.factorize(100))
                        .addPrice("2023-11-20", Values.Quote.factorize(110)).addTo(client);
        fund.getAttributes().put(client.getSettings().getOptionalPartialExemptionAttributeType().orElseThrow(), 0.3);
        new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);

        var recent = calculate(2023, "2.55", LocalDate.parse("2023-11-27")).items().get(0);
        var outdated = calculate(2023, "2.55", LocalDate.parse("2023-12-05")).items().get(0);

        assertThat(recent.hasOutdatedPrices(), is(false));
        assertThat(outdated.hasOutdatedPrices(), is(true));
    }

    @Test
    public void testDistributionsReduceAdvanceLumpSum()
    {
        var fund = fund(0.3, 110);
        new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);
        // gross distribution of 1.00 per share (net 8.00 + taxes 2.00)
        new AccountBuilder().dividend("2023-06-15", amountOf(8), amountOf(2), fund).addTo(client);

        var item = calculate2023().items().get(0);

        assertThat(item.distributionsPerShare().getAmount(), is(Values.Quote.factorize(1)));
        assertThat(item.advanceLumpSum(), is(euro(7.85)));
    }

    @Test
    public void testLocalDistributionBeforePurchaseReducesAdvanceLumpSumOnce()
    {
        var fund = fund(0.3, 110);
        var exDate = LocalDate.parse("2023-02-13");
        var paymentDate = LocalDate.parse("2023-02-15");
        fund.addEvent(new DividendEvent(exDate, paymentDate, euro(1), "first"));
        fund.addEvent(new DividendEvent(exDate, paymentDate, euro(1), "second"));
        fund.addEvent(new DividendEvent(LocalDate.parse("2023-12-29"), LocalDate.parse("2024-01-03"), euro(2),
                        "first"));
        new PortfolioBuilder(account).buy(fund, "2023-07-01", sharesOf(10), amountOf(1000)).addTo(client);

        var item = calculate2023().items().get(0);

        assertThat(item.distributionsPerShare().getAmount(), is(Values.Quote.factorize(1)));
        assertThat(item.advanceLumpSum(), is(euro(3.93)));
    }

    @Test
    public void testLocalDistributionNearBookingWithoutExDateIsNotAdded()
    {
        var fund = fund(0.3, 110);
        fund.addEvent(new DividendEvent(LocalDate.parse("2023-02-13"), LocalDate.parse("2023-02-15"), euro(1),
                        "first"));
        var broker = new AccountBuilder().dividend("2023-02-22", amountOf(10), fund).addTo(client);
        broker.getTransactions().get(0).setShares(sharesOf(10));
        new PortfolioBuilder(broker).buy(fund, "2023-07-01", sharesOf(10), amountOf(1000)).addTo(client);

        var item = calculate2023().items().get(0);

        assertThat(item.distributionsPerShare().getAmount(), is(Values.Quote.factorize(1)));
        assertThat(item.advanceLumpSum(), is(euro(3.93)));
    }

    @Test
    public void testSameDistributionBookedOnDifferentDays()
    {
        var fund = fund(0.3, 110);
        var firstBroker = new AccountBuilder().dividend("2023-01-05", amountOf(10), fund).addTo(client);
        var secondBroker = new AccountBuilder().dividend("2023-01-09", amountOf(20), fund).addTo(client);
        var exDate = LocalDateTime.parse("2022-12-29T00:00");
        var firstPayment = firstBroker.getTransactions().get(0);
        firstPayment.setExDate(exDate);
        firstPayment.setShares(sharesOf(10));
        var secondPayment = secondBroker.getTransactions().get(0);
        secondPayment.setExDate(exDate);
        secondPayment.setShares(sharesOf(20));
        new PortfolioBuilder(firstBroker).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);
        new PortfolioBuilder(secondBroker).buy(fund, "2022-06-01", sharesOf(20), amountOf(2000)).addTo(client);

        var result = calculate2023();

        assertThat(result.items(), hasSize(2));
        for (var item : result.items())
            assertThat(item.distributionsPerShare().getAmount(), is(Values.Quote.factorize(1)));
        assertThat(result.getTotalAdvanceLumpSum(), is(euro(23.55)));
    }

    @Test
    public void testDistributionWithoutExDateBookedByTwoBrokersOnDifferentDaysIsUnresolved()
    {
        var fund = fund(0.3, 110);
        var firstBroker = new AccountBuilder().dividend("2023-01-05", amountOf(10), fund).addTo(client);
        var secondBroker = new AccountBuilder().dividend("2023-01-09", amountOf(20), fund).addTo(client);
        firstBroker.getTransactions().get(0).setShares(sharesOf(10));
        secondBroker.getTransactions().get(0).setShares(sharesOf(20));
        new PortfolioBuilder(firstBroker).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);
        new PortfolioBuilder(secondBroker).buy(fund, "2022-06-01", sharesOf(20), amountOf(2000)).addTo(client);

        var result = calculate2023();

        assertThat(result.items(), hasSize(2));
        for (var item : result.items())
            assertThat(item.hasUnresolvedDistributions(), is(true));
        assertThat(result.getTotalTax(), is(euro(0)));
    }

    @Test
    public void testDistributionsWithoutExDateBookedByTwoBrokersOnTheSameTwoDaysAreAdditive()
    {
        var fund = fund(0.3, 110);
        var firstBroker = new AccountBuilder().dividend("2023-01-05", amountOf(2.5), fund)
                        .dividend("2023-01-09", amountOf(7.5), fund).addTo(client);
        var secondBroker = new AccountBuilder().dividend("2023-01-05", amountOf(5), fund)
                        .dividend("2023-01-09", amountOf(15), fund).addTo(client);
        firstBroker.getTransactions().forEach(tx -> tx.setShares(sharesOf(10)));
        secondBroker.getTransactions().forEach(tx -> tx.setShares(sharesOf(20)));
        new PortfolioBuilder(firstBroker).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);
        new PortfolioBuilder(secondBroker).buy(fund, "2022-06-01", sharesOf(20), amountOf(2000)).addTo(client);

        var result = calculate2023();

        assertThat(result.items(), hasSize(2));
        for (var item : result.items())
        {
            assertThat(item.hasUnresolvedDistributions(), is(false));
            assertThat(item.distributionsPerShare().getAmount(), is(Values.Quote.factorize(1)));
        }
        // (1.785 - 1.00) * 30 shares
        assertThat(result.getTotalAdvanceLumpSum(), is(euro(23.55)));
    }

    @Test
    public void testDistributionsWithoutExDateAreAdditiveWithinOneAccount()
    {
        var fund = fund(0.3, 110);
        new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);
        var dividends = new AccountBuilder().dividend("2023-01-05", amountOf(5), fund)
                        .dividend("2023-01-20", amountOf(5), fund).addTo(client);
        dividends.getTransactions().forEach(tx -> tx.setShares(sharesOf(10)));

        var item = calculate2023().items().get(0);

        assertThat(item.hasUnresolvedDistributions(), is(false));
        assertThat(item.distributionsPerShare().getAmount(), is(Values.Quote.factorize(1)));
    }

    @Test
    public void testDistributionWithoutSharesUsesHoldingsBeforeExDate()
    {
        var fund = fund(0.3, 105);
        new PortfolioBuilder(account) //
                        .buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)) //
                        .sell(fund, "2023-06-01", sharesOf(5), amountOf(500)).addTo(client);
        var dividendAccount = new AccountBuilder().dividend("2023-06-15", amountOf(5), 0, fund).addTo(client);
        dividendAccount.getTransactions().get(0).setExDate(LocalDateTime.parse("2023-06-01T00:00"));

        var item = calculate2023().items().get(0);

        assertThat(item.distributionsPerShare().getAmount(), is(Values.Quote.factorize(0.5)));
        assertThat(item.advanceLumpSum(), is(euro(6.43)));
    }

    @Test
    public void testNoAdvanceLumpSumInLossYear()
    {
        var fund = fund(0.3, 95);
        new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);

        var item = calculate2023().items().get(0);

        assertThat(item.advanceLumpSum(), is(euro(0)));
    }

    @Test
    public void testGainLimitUsesFirstPriceInCalendarYear()
    {
        var fund = fund(0.3, 103);
        fund.addPrice(new SecurityPrice(LocalDate.parse("2023-01-02"), Values.Quote.factorize(102)));
        new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);

        var item = calculate2023().items().get(0);

        assertThat(item.advanceLumpSum(), is(euro(10)));
    }

    @Test
    public void testNoAdvanceLumpSumWithNegativeBaseInterestRate()
    {
        var fund = fund(0.3, 110);
        new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);

        var item = calculate(2023, "-0.45", END_OF_2023).items().get(0);

        assertThat(item.advanceLumpSum(), is(euro(0)));
    }

    @Test
    public void testPurchaseDuringYearIsReducedPerMonth()
    {
        var fund = fund(0.3, 110);
        new PortfolioBuilder(account).buy(fund, "2023-03-15", sharesOf(10), amountOf(1000)).addTo(client);

        var item = calculate2023().items().get(0);

        // 17.85 * 10/12
        assertThat(item.advanceLumpSum(), is(euro(14.88)));
    }

    @Test
    public void testPartialSaleUsesFifo()
    {
        var fund = fund(0, 110);
        new PortfolioBuilder(account) //
                        .buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)) //
                        .buy(fund, "2023-07-10", sharesOf(10), amountOf(1000)) //
                        .sell(fund, "2023-09-01", sharesOf(15), amountOf(1500)) //
                        .addTo(client);

        var item = calculate2023().items().get(0);

        // remaining 5 shares were acquired in July: 5 * 1.785 * 6/12
        assertThat(item.shares(), is(sharesOf(5)));
        assertThat(item.advanceLumpSum(), is(euro(4.46)));
        assertThat(item.taxableAmount(), is(euro(4.46)));
    }

    @Test
    public void testSaleAfterTransferUsesOriginalAcquisitionDateForFifo()
    {
        var fund = fund(0.3, 110);
        var source = new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000))
                        .addTo(client);
        var target = new PortfolioBuilder(account) //
                        .buy(fund, "2023-04-01", sharesOf(10), amountOf(1000)) //
                        .sell(fund, "2023-06-01", sharesOf(10), amountOf(1000)) //
                        .addTo(client);

        var transfer = new PortfolioTransferEntry(source, target);
        transfer.setDate(LocalDateTime.parse("2023-05-01T00:00"));
        transfer.setSecurity(fund);
        transfer.setShares(sharesOf(10));
        transfer.setAmount(amountOf(1000));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.insert();

        var result = calculate2023();

        assertThat(result.items(), hasSize(1));
        var item = result.items().get(0);
        assertThat(item.portfolio(), is(target));
        assertThat(item.advanceLumpSum(), is(euro(13.39)));
    }

    @Test
    public void testItemsPerPortfolio()
    {
        var fund = fund(0.3, 110);
        Portfolio a = new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000))
                        .addTo(client);
        Portfolio b = new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(20), amountOf(2000))
                        .addTo(client);

        var result = calculate2023();

        var byPortfolio = result.getItemsByPortfolio();
        assertThat(byPortfolio.get(a).get(0).advanceLumpSum(), is(euro(17.85)));
        assertThat(byPortfolio.get(b).get(0).advanceLumpSum(), is(euro(35.70)));
        assertThat(result.getTotalAdvanceLumpSum(), is(euro(53.55)));
    }

    @Test
    public void testExemptionOrderReducesTaxableAmount()
    {
        var fund = fund(0.3, 110);
        var portfolio = new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000))
                        .addTo(client);

        var item = calculate(2023, "2.55", END_OF_2023, Map.of(portfolio, exemptionOf(10))).items().get(0);

        assertThat(item.taxableAmount(), is(euro(2.50)));
        assertThat(item.usedExemption(), is(euro(10)));
        assertThat(item.tax(), is(euro(0.66)));
    }

    @Test
    public void testExemptionOrderLargerThanTaxableAmountIsPartlyUsed()
    {
        var fund = fund(0.3, 110);
        var portfolio = new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000))
                        .addTo(client);

        var item = calculate(2023, "2.55", END_OF_2023, Map.of(portfolio, exemptionOf(1000))).items().get(0);

        assertThat(item.taxableAmount(), is(euro(0)));
        assertThat(item.usedExemption(), is(euro(12.50)));
        assertThat(item.tax(), is(euro(0)));
    }

    @Test
    public void testExemptionOrderIsConsumedItemByItemPerPortfolio()
    {
        var first = fund(0.3, 110);
        var second = fund(0.3, 110);
        var portfolio = new PortfolioBuilder(account) //
                        .buy(first, "2022-06-01", sharesOf(10), amountOf(1000)) //
                        .buy(second, "2022-06-01", sharesOf(10), amountOf(1000)) //
                        .addTo(client);

        var result = calculate(2023, "2.55", END_OF_2023, Map.of(portfolio, exemptionOf(20)));

        assertThat(result.items(), hasSize(2));
        // taxable amount is 12.50 per fund, the order of 20.00 is used up
        assertThat(result.getTotalUsedExemption(), is(euro(20)));
        assertThat(result.getTotalTaxableAmount(), is(euro(5)));
        assertThat(result.getTotalTax(), is(euro(1.32)));
    }

    @Test
    public void testExemptionOrderAppliesToItsPortfolioOnly()
    {
        var fund = fund(0.3, 110);
        var withoutOrder = new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000))
                        .addTo(client);
        var withOrder = new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000))
                        .addTo(client);

        var result = calculate(2023, "2.55", END_OF_2023, Map.of(withOrder, exemptionOf(10)));

        assertThat(result.getItemsByPortfolio().get(withoutOrder).get(0).usedExemption(), is(euro(0)));
        assertThat(result.getItemsByPortfolio().get(withOrder).get(0).usedExemption(), is(euro(10)));
        assertThat(result.getTotalUsedExemption(), is(euro(10)));
    }

    @Test
    public void testZeroExemptionOrderLeavesTaxableAmountUnchanged()
    {
        var fund = fund(0.3, 110);
        var portfolio = new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000))
                        .addTo(client);

        var item = calculate(2023, "2.55", END_OF_2023, Map.of(portfolio, 0L)).items().get(0);

        assertThat(item.usedExemption(), is(euro(0)));
        assertThat(item.taxableAmount(), is(euro(12.50)));
    }

    @Test
    public void testGeneralLossBalanceIsExhaustedAcrossFundsBeforeExemptionOrder()
    {
        var first = fund(0.3, 110);
        var second = fund(0.3, 110);
        var portfolio = new PortfolioBuilder(account) //
                        .buy(first, "2022-06-01", sharesOf(10), amountOf(1000)) //
                        .buy(second, "2022-06-01", sharesOf(10), amountOf(1000)) //
                        .addTo(client);

        var result = calculate(2023, "2.55", END_OF_2023, Map.of(portfolio, amountOf(20)),
                        Map.of(portfolio, amountOf(10)));

        assertThat(result.getTotalUsedLoss(), is(euro(20)));
        assertThat(result.items().getFirst().usedExemption(), is(euro(0)));
        assertThat(result.getTotalUsedExemption(), is(euro(5)));
        assertThat(result.getTotalTaxableAmount(), is(euro(0)));
        assertThat(result.getTotalTax(), is(euro(0)));
    }

    @Test
    public void testUnusedLossBalanceDoesNotSpillIntoAnotherPortfolio()
    {
        var fund = fund(0.3, 110);
        var withLoss = new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);
        var withoutLoss = new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000))
                        .addTo(client);

        var result = calculate(2023, "2.55", END_OF_2023, Map.of(withLoss, amountOf(20)),
                        Map.of(withLoss, amountOf(10)));

        assertThat(result.getTotalUsedLoss(), is(euro(12.50)));
        assertThat(result.getTotalUsedExemption(), is(euro(0)));
        assertThat(result.getItemsByPortfolio().get(withoutLoss).getFirst().taxableAmount(), is(euro(12.50)));
    }

    private static long exemptionOf(double amount)
    {
        return Values.Amount.factorize(amount);
    }

    @Test
    public void testSaleAtTransferTimestampInEitherPortfolioOrder()
    {
        for (boolean targetFirst : new boolean[] { false, true })
        {
            setUp();
            var fund = fund(0.3, 110);
            var source = new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(1), amountOf(100));
            var target = new PortfolioBuilder(account).sell(fund, "2023-06-01", sharesOf(1), amountOf(110));
            var first = (targetFirst ? target : source).addTo(client);
            var second = (targetFirst ? source : target).addTo(client);

            var transfer = new PortfolioTransferEntry(targetFirst ? second : first, targetFirst ? first : second);
            transfer.setDate(LocalDateTime.parse("2023-06-01T00:00"));
            transfer.setSecurity(fund);
            transfer.setShares(sharesOf(1));
            transfer.setAmount(amountOf(110));
            transfer.setCurrencyCode(CurrencyUnit.EUR);
            transfer.insert();

            assertThat(calculate2023().items(), hasSize(0));
        }
    }

    @Test
    public void testPostYearCutoffDoesNotIncludeNextYearPurchasesOrPrices()
    {
        var fund = fund(0.3, 110);
        fund.addPrice(new SecurityPrice(LocalDate.parse("2024-01-03"), Values.Quote.factorize(90)));
        new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000))
                        .buy(fund, "2024-01-03", sharesOf(10), amountOf(900)).addTo(client);

        assertThat(calculate(2023, "2.55", LocalDate.parse("2024-01-31")), is(calculate2023()));
    }

    @Test
    public void testTaxEstimateDoesNotDependOnClientCurrency()
    {
        var fund = new SecurityBuilder().addPrice("2014-12-31", Values.Quote.factorize(100))
                        .addPrice("2015-01-02", Values.Quote.factorize(100))
                        .addPrice("2015-01-16", Values.Quote.factorize(102)).addTo(client);
        var attribute = client.getSettings().getOptionalPartialExemptionAttributeType().orElseThrow();
        fund.getAttributes().put(attribute, 0.3);
        new PortfolioBuilder(account).buy(fund, "2014-06-01", sharesOf(10), amountOf(1000)).addTo(client);
        var rate = new BaseInterestRate(2015, BigDecimal.ONE, Source.MANUAL);
        var cutoff = LocalDate.parse("2015-01-16");
        var expected = new AdvanceLumpSumCalculation(client, new TestCurrencyConverter()).calculate(rate, TAX_RATE,
                        cutoff, Map.of(), Map.of());

        client.setBaseCurrency(CurrencyUnit.USD);
        var result = new AdvanceLumpSumCalculation(client, new TestCurrencyConverter(client.getBaseCurrency()))
                        .calculate(rate, TAX_RATE, cutoff, Map.of(), Map.of());

        assertThat(expected.getTotalTax(), is(euro(1.29)));
        assertThat(result, is(expected));
    }

    @Test
    public void testNonzeroDividendWithUnknownSharesDoesNotProduceTaxEstimate()
    {
        var fund = fund(0.3, 110);
        new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000))
                        .sell(fund, "2023-06-01", sharesOf(10), amountOf(1100))
                        .buy(fund, "2023-07-01", sharesOf(10), amountOf(1100)).addTo(client);
        new AccountBuilder().dividend("2023-06-15", amountOf(1), 0, fund).addTo(client);

        var item = calculate2023().items().getFirst();

        assertThat(item.hasUnresolvedDistributions(), is(true));
        assertThat(item.hasMissingExchangeRates(), is(false));
        assertThat(item.tax(), is(euro(0)));
    }

    @Test
    public void testSecurityWithoutPartialExemptionIsIgnored()
    {
        var stock = new SecurityBuilder() //
                        .addPrice("2022-12-30", Values.Quote.factorize(100)) //
                        .addPrice("2023-12-29", Values.Quote.factorize(110)) //
                        .addTo(client);
        new PortfolioBuilder(account).buy(stock, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);

        assertThat(calculate2023().items(), hasSize(0));
    }

    @Test
    public void testSoldPositionIsIgnored()
    {
        var fund = fund(0.3, 110);
        new PortfolioBuilder(account) //
                        .buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)) //
                        .sell(fund, "2023-12-28", sharesOf(10), amountOf(1100)) //
                        .addTo(client);

        assertThat(calculate2023().items(), hasSize(0));
    }

    @Test
    public void testPriorYearPriceDoesNotCountAsPriceInValuationYear()
    {
        var fund = new SecurityBuilder().addPrice("2022-12-30", Values.Quote.factorize(100))
                        .addPrice("2024-01-02", Values.Quote.factorize(110)).addTo(client);
        var attribute = client.getSettings().getOptionalPartialExemptionAttributeType().orElseThrow();
        fund.getAttributes().put(attribute, 0.3);
        new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);

        var item = calculate2023().items().get(0);

        assertThat(item.hasMissingPrices(), is(true));
        assertThat(item.priceStart().getAmount(), is(Values.Quote.factorize(100)));
        assertThat(item.priceEnd().getAmount(), is(0L));
        assertThat(item.advanceLumpSum(), is(euro(0)));
    }

    @Test
    public void testEstimateIgnoresTransactionsAfterValuationDate()
    {
        var fund = fund(0.3, 110);
        fund.addPrice(new SecurityPrice(LocalDate.parse("2023-06-30"), Values.Quote.factorize(110)));
        new PortfolioBuilder(account) //
                        .buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)) //
                        .buy(fund, "2023-07-01", sharesOf(10), amountOf(1100)) //
                        .addTo(client);
        new AccountBuilder().dividend("2023-07-01", amountOf(20), 0, fund).addTo(client);

        var item = calculate(2023, "2.55", LocalDate.parse("2023-06-30")).items().get(0);

        assertThat(item.shares(), is(sharesOf(10)));
        assertThat(item.distributionsPerShare().getAmount(), is(0L));
        assertThat(item.advanceLumpSum(), is(euro(17.85)));
    }

    @Test
    public void testForeignPricesAndDistributionsUseTheirOwnDates()
    {
        var fund = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice("2014-12-31", Values.Quote.factorize(115.88)) //
                        .addPrice("2015-01-02", Values.Quote.factorize(115.88)) //
                        .addPrice("2015-01-16", Values.Quote.factorize(127.468)) //
                        .addTo(client);
        var attribute = client.getSettings().getOptionalPartialExemptionAttributeType().orElseThrow();
        fund.getAttributes().put(attribute, 0.3);
        new PortfolioBuilder(account).buy(fund, "2014-06-01", sharesOf(10), amountOf(1000)).addTo(client);
        new AccountBuilder(CurrencyUnit.USD).dividend("2015-01-05", amountOf(1.19), 0, fund).addTo(client);

        var item = calculate(2015, "1.00", LocalDate.parse("2015-01-16")).items().get(0);

        assertThat(item.priceEnd().getAmount(), is(Values.Quote.factorize(110)));
        // USD 1.19 converts to EUR 1.00 on the payment date, not EUR 1.03 at year end.
        assertThat(item.distributionsPerShare().getAmount(), is(Values.Quote.factorize(0.1)));
        // USD 115.88 at 1.2141 on the opening quote date yields a base return of EUR 6.68.
        assertThat(item.advanceLumpSum(), is(euro(5.68)));
    }

    @Test
    public void testMissingExchangeRateDoesNotProduceTaxEstimate()
    {
        var fund = fund(0.3, 110);
        fund.setCurrencyCode("XTS");
        new PortfolioBuilder(account).buy(fund, "2022-06-01", sharesOf(10), amountOf(1000)).addTo(client);
        var converter = new CurrencyConverterImpl(new ExchangeRateProviderFactory(client), CurrencyUnit.EUR);
        var rate = new BaseInterestRate(2023, new BigDecimal("2.55"), Source.MANUAL);

        var item = new AdvanceLumpSumCalculation(client, converter)
                        .calculate(rate, TAX_RATE, END_OF_2023, Map.of(), Map.of())
                        .items().getFirst();

        assertThat(item.hasMissingExchangeRates(), is(true));
        assertThat(item.tax(), is(euro(0)));
    }
}
