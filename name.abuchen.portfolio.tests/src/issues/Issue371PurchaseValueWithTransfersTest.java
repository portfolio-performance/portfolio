package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

public class Issue371PurchaseValueWithTransfersTest
{
    @Test
    public void testPurchaseValueOfSecurityPositionWithTransfers() throws IOException
    {
        Client client = ClientFactory.load(Issue371PurchaseValueWithTransfersTest.class
                        .getResourceAsStream("Issue371PurchaseValueWithTransfers.xml")); //$NON-NLS-1$

        Security adidas = client.getSecurities().get(0);
        assertThat(adidas.getName(), is("Adidas AG")); //$NON-NLS-1$

        Interval period = Interval.of(LocalDate.parse("2010-11-20"), //$NON-NLS-1$
                        LocalDate.parse("2015-11-20")); //$NON-NLS-1$

        // make sure that the transfer entry exists
        assertThat(client.getPortfolios().size(), is(2));
        assertThat(client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .filter(t -> t.getSecurity() == adidas)
                        .filter(t -> t.getCrossEntry() instanceof PortfolioTransferEntry)
                        .anyMatch(t -> t.getType() == PortfolioTransaction.Type.TRANSFER_IN), is(true));

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, period.getEnd());
        SecurityPosition securityPosition = snapshot.getPositionsByVehicle().get(adidas).getPosition();

        LazySecurityPerformanceSnapshot securitySnapshot = LazySecurityPerformanceSnapshot.create(client, converter,
                        period);

        LazySecurityPerformanceRecord record = securitySnapshot.getRecords().get(0);
        assertThat(record.getSecurity(), is(adidas));

        assertThat(securityPosition.getShares(), is(record.getSharesHeld()));
        assertThat(securityPosition.calculateValue(), is(record.getMarketValue()));

        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2397.6))));

        // pinned values previously verified via SecurityPerformanceSnapshotComparator
        assertThat(record.getMarketValue(), is(Money.of("EUR", 414023L))); //$NON-NLS-1$
        assertThat(record.getQuote(), is(Quote.of("EUR", 8809000000L))); //$NON-NLS-1$
        assertThat(record.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        is(Money.of("EUR", 239760L))); //$NON-NLS-1$
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO), is(Quote.of("EUR", 5080000000L))); //$NON-NLS-1$
        assertThat(record.getFees(), is(Money.of("EUR", 1000L))); //$NON-NLS-1$
        assertThat(record.getTaxes(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getDelta(), is(Money.of("EUR", 174263L))); //$NON-NLS-1$
        assertThat(record.getDeltaPercent(), closeTo(0.7268226559893226, 0.0001));
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.FIFO), is(Money.of("EUR", 174263L))); //$NON-NLS-1$
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.MOVING_AVERAGE), is(Money.of("EUR", 174263L))); //$NON-NLS-1$
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.FIFO), closeTo(0.7268226559893227, 0.0001));
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.MOVING_AVERAGE),
                        closeTo(0.7268226559893227, 0.0001));
        assertThat(record.getIrr(), closeTo(0.1509824601974703, 0.0001));
        assertThat(record.getTrueTimeWeightedRateOfReturn(), closeTo(0.7268226559893225, 0.0001));
        assertThat(record.getTrueTimeWeightedRateOfReturnAnnualized(), closeTo(0.11538182151129361, 0.0001));
        assertThat(record.getDrawdown().getMaxDrawdown(), closeTo(0.003300330033003359, 0.0001));
        assertThat(record.getDrawdown().getMaxDrawdownDuration().getDays(), is(1410L));
        assertThat(record.getVolatility().getStandardDeviation(), closeTo(0.5133051461982191, 0.0001));
        assertThat(record.getVolatility().getSemiDeviation(), closeTo(0.017855872674043684, 0.0001));
        assertThat(record.getSumOfDividends(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getDividendEventCount(), is(0));
        assertThat(record.getLastDividendPayment(), is((LocalDate) null));
        assertThat(record.getPeriodicity(), is(BaseSecurityPerformanceRecord.Periodicity.NONE));
        assertThat(record.getTotalRateOfReturnDiv(CostMethod.FIFO), closeTo(0.0, 0.0001));
        assertThat(record.getTotalRateOfReturnDiv(CostMethod.MOVING_AVERAGE), closeTo(0.0, 0.0001));
        assertThat(record.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains(),
                        is(Money.of("EUR", 175263L))); //$NON-NLS-1$
        assertThat(record.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of("EUR", 175263L))); //$NON-NLS-1$
    }
}
