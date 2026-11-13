package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.FundTransferEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class FundTransferCalculationTest
{
    @Test
    public void testFundTransferUpdatesHoldingsAndCarriesFifoCost()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);
        Security targetFund = new SecurityBuilder().addPrice("2020-12-31", Values.Quote.factorize(100)).addTo(client);

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);

        Portfolio targetPortfolio = new PortfolioBuilder().addTo(client);

        PortfolioTransaction sourceBuy = sourcePortfolio.getTransactions().get(0);
        insertFundTransfer(sourcePortfolio, targetPortfolio, sourceFund, targetFund, Values.Share.factorize(5),
                        Values.Share.factorize(8), Values.Amount.factorize(750),
                        new FundTransferEntry.CarriedLot(LocalDate.parse("2020-01-01"),
                                        Values.Share.factorize(5), Values.Share.factorize(8),
                                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500)),
                                        sourceBuy.getUUID()));

        LazySecurityPerformanceSnapshot snapshot = createSnapshot(client);
        LazySecurityPerformanceRecord sourceRecord = snapshot.getRecord(sourceFund)
                        .orElseThrow(IllegalArgumentException::new);
        LazySecurityPerformanceRecord targetRecord = snapshot.getRecord(targetFund)
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(calculateSharesHeld(sourceRecord), is(Values.Share.factorize(5)));
        assertThat(calculateSharesHeld(targetRecord), is(Values.Share.factorize(8)));

        assertThat(sourceRecord.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));
        assertThat(targetRecord.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));
    }

    @Test
    public void testFundTransferCanCarryMultipleLotsIntoTargetFifoCost()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);
        Security targetFund = new SecurityBuilder().addPrice("2020-12-31", Values.Quote.factorize(100)).addTo(client);

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .buy(sourceFund, "2020-03-01", Values.Share.factorize(6),
                                        Values.Amount.factorize(600)) //
                        .addTo(client);

        Portfolio targetPortfolio = new PortfolioBuilder().addTo(client);

        PortfolioTransaction firstBuy = sourcePortfolio.getTransactions().get(0);
        PortfolioTransaction secondBuy = sourcePortfolio.getTransactions().get(1);
        insertFundTransfer(sourcePortfolio, targetPortfolio, sourceFund, targetFund, Values.Share.factorize(7),
                        Values.Share.factorize(11), Values.Amount.factorize(1050),
                        new FundTransferEntry.CarriedLot(LocalDate.parse("2020-01-01"),
                                        Values.Share.factorize(5), Values.Share.factorize(8),
                                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500)), firstBuy.getUUID()),
                        new FundTransferEntry.CarriedLot(LocalDate.parse("2020-03-01"),
                                        Values.Share.factorize(2), Values.Share.factorize(3),
                                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200)), secondBuy.getUUID()));

        LazySecurityPerformanceSnapshot snapshot = createSnapshot(client);
        LazySecurityPerformanceRecord sourceRecord = snapshot.getRecord(sourceFund)
                        .orElseThrow(IllegalArgumentException::new);
        LazySecurityPerformanceRecord targetRecord = snapshot.getRecord(targetFund)
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(calculateSharesHeld(sourceRecord), is(Values.Share.factorize(9)));
        assertThat(calculateSharesHeld(targetRecord), is(Values.Share.factorize(11)));

        assertThat(sourceRecord.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(900))));
        assertThat(targetRecord.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(700))));
    }

    @Test
    public void testFundTransferDefersCapitalGainsUntilTargetSale()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);
        Security targetFund = new SecurityBuilder().addPrice("2020-12-31", Values.Quote.factorize(100)).addTo(client);

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);

        Portfolio targetPortfolio = new PortfolioBuilder().addTo(client);

        PortfolioTransaction sourceBuy = sourcePortfolio.getTransactions().get(0);
        insertFundTransfer(sourcePortfolio, targetPortfolio, sourceFund, targetFund, Values.Share.factorize(10),
                        Values.Share.factorize(15), Values.Amount.factorize(1500),
                        new FundTransferEntry.CarriedLot(LocalDate.parse("2020-01-01"),
                                        Values.Share.factorize(10), Values.Share.factorize(15),
                                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)),
                                        sourceBuy.getUUID()));

        LazySecurityPerformanceSnapshot snapshot = createSnapshot(client);
        LazySecurityPerformanceRecord sourceRecord = snapshot.getRecord(sourceFund)
                        .orElseThrow(IllegalArgumentException::new);
        LazySecurityPerformanceRecord targetRecord = snapshot.getRecord(targetFund)
                        .orElseThrow(IllegalArgumentException::new);

        assertThat(sourceRecord.getDelta(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));
        assertThat(targetRecord.getDelta(), is(Money.of(CurrencyUnit.EUR, 0)));

        assertThat(sourceRecord.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains(),
                        is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(sourceRecord.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of(CurrencyUnit.EUR, 0)));

        assertThat(targetRecord.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));
        assertThat(targetRecord.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));

        PortfolioTransaction sale = new PortfolioTransaction();
        sale.setType(PortfolioTransaction.Type.SELL);
        sale.setDateTime(LocalDateTime.parse("2021-01-01T00:00"));
        sale.setSecurity(targetFund);
        sale.setShares(Values.Share.factorize(15));
        sale.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1800)));
        targetPortfolio.addTransaction(sale);

        LazySecurityPerformanceRecord targetRecordAfterSale = LazySecurityPerformanceSnapshot
                        .create(client, new TestCurrencyConverter(),
                                        Interval.of(LocalDate.parse("2019-12-31"), LocalDate.parse("2021-12-31")))
                        .getRecord(targetFund).orElseThrow(IllegalArgumentException::new);

        assertThat(targetRecordAfterSale.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(800))));
        assertThat(targetRecordAfterSale.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(800))));
    }

    private LazySecurityPerformanceSnapshot createSnapshot(Client client)
    {
        return LazySecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(),
                        Interval.of(LocalDate.parse("2019-12-31"), LocalDate.parse("2020-12-31")));
    }

    private long calculateSharesHeld(LazySecurityPerformanceRecord record)
    {
        SharesHeldCalculation calculation = Calculation.perform(SharesHeldCalculation.class, new TestCurrencyConverter(),
                        record.getSecurity(), record.getLineItems());
        return calculation.getSharesHeld();
    }

    private void insertFundTransfer(Portfolio sourcePortfolio, Portfolio targetPortfolio, Security sourceFund,
                    Security targetFund, long sourceShares, long targetShares, long transferAmount,
                    FundTransferEntry.CarriedLot... lots)
    {
        FundTransferEntry entry = new FundTransferEntry(sourcePortfolio, targetPortfolio);
        entry.setDate(LocalDateTime.parse("2020-06-01T00:00"));
        entry.setSourceSecurity(sourceFund);
        entry.setTargetSecurity(targetFund);
        entry.setSourceShares(sourceShares);
        entry.setTargetShares(targetShares);
        entry.setSourceMonetaryAmount(Money.of(CurrencyUnit.EUR, transferAmount));
        entry.setTargetMonetaryAmount(Money.of(CurrencyUnit.EUR, transferAmount));

        for (FundTransferEntry.CarriedLot lot : lots)
            entry.addCarriedLot(lot);

        entry.insert();
    }
}
