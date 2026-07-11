package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.FundTransferEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class FundTransferPerformanceIndexTest
{
    private static final double PRECISION = 0.000000001d;

    @Test
    public void testFundTransferIsPerformanceNeutralForTTWROR()
    {
        TransferScenario scenario = createTransferScenario();
        Interval period = Interval.of(scenario.startDate, scenario.endDate);

        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index = PerformanceIndex.forClient(scenario.client, new TestCurrencyConverter(), period,
                        warnings);

        assertThat(warnings.isEmpty(), is(true));

        int transferIndex = Dates.daysBetween(period.getStart(), scenario.transferDate);
        assertThat(index.getInboundTransferals()[transferIndex], is(0L));
        assertThat(index.getOutboundTransferals()[transferIndex], is(0L));

        // The fund transfer changes the security, but it must not create a
        // TTWROR cash flow. The result is the uninterrupted economic return:
        // EUR 1,000 initial value -> EUR 1,800 final value.
        assertThat(index.getFinalAccumulatedPercentage(), IsCloseTo.closeTo(0.8d, PRECISION));
    }

    @Test
    public void testFundTransferIsNotReportedAsTransferInPerformanceSnapshot()
    {
        TransferScenario scenario = createTransferScenario();
        Interval period = Interval.of(scenario.startDate, scenario.endDate);

        PerformanceIndex index = PerformanceIndex.forClient(scenario.client, new TestCurrencyConverter(), period,
                        new ArrayList<>());
        ClientPerformanceSnapshot snapshot = index.getClientPerformanceSnapshot().orElseThrow();

        assertThat(snapshot.getValue(CategoryType.INITIAL_VALUE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(snapshot.getValue(CategoryType.TRANSFERS), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1800))));
    }

    @Test
    public void testTargetOnlyFundTransferIsAnExternalInboundFlow()
    {
        TransferScenario scenario = createTransferScenario();
        Client targetOnly = new PortfolioClientFilter(scenario.targetPortfolio).filter(scenario.client);
        Interval period = Interval.of(scenario.startDate, scenario.endDate);

        PerformanceIndex index = PerformanceIndex.forClient(targetOnly, new TestCurrencyConverter(), period,
                        new ArrayList<>());
        int transferIndex = Dates.daysBetween(period.getStart(), scenario.transferDate);

        assertThat(index.getInboundTransferals()[transferIndex], is(Values.Amount.factorize(1500)));
        assertThat(index.getOutboundTransferals()[transferIndex], is(0L));
        assertThat(index.getFinalAccumulatedPercentage(), IsCloseTo.closeTo(0.2d, PRECISION));
        assertThat(index.getClientPerformanceSnapshot().orElseThrow().getValue(CategoryType.TRANSFERS),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500))));
    }

    @Test
    public void testSourceOnlyFundTransferIsAnExternalOutboundFlow()
    {
        TransferScenario scenario = createTransferScenario();
        Client sourceOnly = new PortfolioClientFilter(scenario.sourcePortfolio).filter(scenario.client);
        Interval period = Interval.of(scenario.startDate, scenario.endDate);

        PerformanceIndex index = PerformanceIndex.forClient(sourceOnly, new TestCurrencyConverter(), period,
                        new ArrayList<>());
        int transferIndex = Dates.daysBetween(period.getStart(), scenario.transferDate);

        assertThat(index.getInboundTransferals()[transferIndex], is(0L));
        assertThat(index.getOutboundTransferals()[transferIndex], is(Values.Amount.factorize(1500)));
        assertThat(index.getFinalAccumulatedPercentage(), IsCloseTo.closeTo(0.5d, PRECISION));
        assertThat(index.getClientPerformanceSnapshot().orElseThrow().getValue(CategoryType.TRANSFERS),
                        is(Money.of(CurrencyUnit.EUR, -Values.Amount.factorize(1500))));
    }

    @Test
    public void testTargetOnlyFundTransferIsAnExternalFlowForIrr()
    {
        TransferScenario scenario = createTransferScenario();
        Client targetOnly = new PortfolioClientFilter(scenario.targetPortfolio).filter(scenario.client);
        TestCurrencyConverter converter = new TestCurrencyConverter();

        ClientIRRYield yield = ClientIRRYield.create(targetOnly,
                        ClientSnapshot.create(targetOnly, converter, scenario.startDate),
                        ClientSnapshot.create(targetOnly, converter, scenario.endDate));

        double expected = IRR.calculate(List.of(scenario.transferDate, scenario.endDate), List.of(-1500d, 1800d));
        assertThat(yield.getIrr(), IsCloseTo.closeTo(expected, PRECISION));
    }

    private TransferScenario createTransferScenario()
    {
        TransferScenario scenario = new TransferScenario();
        scenario.startDate = LocalDate.parse("2020-01-01");
        scenario.transferDate = LocalDate.parse("2020-06-01");
        scenario.endDate = LocalDate.parse("2020-12-31");
        scenario.client = new Client();

        Security sourceFund = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-06-01", Values.Quote.factorize(150)) //
                        .addTo(scenario.client);
        Security targetFund = new SecurityBuilder() //
                        .addPrice("2020-06-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-12-31", Values.Quote.factorize(120)) //
                        .addTo(scenario.client);

        Portfolio sourcePortfolio = new PortfolioBuilder(new AccountBuilder().addTo(scenario.client)) //
                        .inbound_delivery(sourceFund, "2020-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(scenario.client);
        Portfolio targetPortfolio = new PortfolioBuilder(new AccountBuilder().addTo(scenario.client))
                        .addTo(scenario.client);
        scenario.sourcePortfolio = sourcePortfolio;
        scenario.targetPortfolio = targetPortfolio;

        PortfolioTransaction sourceDelivery = sourcePortfolio.getTransactions().get(0);
        insertFundTransfer(sourcePortfolio, targetPortfolio, sourceFund, targetFund,
                        new FundTransferEntry.CarriedLot(LocalDate.parse("2020-01-01"),
                                        Values.Share.factorize(10), Values.Share.factorize(15),
                                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)),
                                        sourceDelivery.getUUID()));

        return scenario;
    }

    private void insertFundTransfer(Portfolio sourcePortfolio, Portfolio targetPortfolio, Security sourceFund,
                    Security targetFund, FundTransferEntry.CarriedLot lot)
    {
        FundTransferEntry entry = new FundTransferEntry(sourcePortfolio, targetPortfolio);
        entry.setDate(LocalDateTime.parse("2020-06-01T00:00"));
        entry.setSourceSecurity(sourceFund);
        entry.setTargetSecurity(targetFund);
        entry.setSourceShares(Values.Share.factorize(10));
        entry.setTargetShares(Values.Share.factorize(15));
        entry.setSourceMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500)));
        entry.setTargetMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500)));
        entry.addCarriedLot(lot);
        entry.insert();
    }

    private static class TransferScenario
    {
        private Client client;
        private LocalDate startDate;
        private LocalDate transferDate;
        private LocalDate endDate;
        private Portfolio sourcePortfolio;
        private Portfolio targetPortfolio;
    }
}
