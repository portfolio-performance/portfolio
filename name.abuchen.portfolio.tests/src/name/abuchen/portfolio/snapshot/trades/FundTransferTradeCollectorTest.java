package name.abuchen.portfolio.snapshot.trades;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.FundTransferEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.SnapshotCache;

@SuppressWarnings("nls")
public class FundTransferTradeCollectorTest
{
    @Test
    public void testFundTransferCarriesTradeStartDateAndEntryValue() throws TradeCollectorException
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);
        Security targetFund = new SecurityBuilder().addTo(client);

        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(sourceFund, "2020-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);
        Portfolio targetPortfolio = new PortfolioBuilder().addTo(client);

        PortfolioTransaction sourceBuy = sourcePortfolio.getTransactions().get(0);
        insertFundTransfer(sourcePortfolio, targetPortfolio, sourceFund, targetFund,
                        LocalDateTime.parse("2020-06-01T00:00"), Values.Share.factorize(10),
                        Values.Share.factorize(15), Values.Amount.factorize(1500),
                        new FundTransferEntry.CarriedLot(LocalDate.parse("2020-01-01"),
                                        Values.Share.factorize(10), Values.Share.factorize(15),
                                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)),
                                        sourceBuy.getUUID()));

        PortfolioTransaction sale = new PortfolioTransaction();
        sale.setType(PortfolioTransaction.Type.SELL);
        sale.setDateTime(LocalDateTime.parse("2021-01-01T00:00"));
        sale.setSecurity(targetFund);
        sale.setShares(Values.Share.factorize(15));
        sale.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1800)));
        targetPortfolio.addTransaction(sale);

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        assertThat(collector.collect(sourceFund).size(), is(0));

        List<Trade> trades = collector.collect(targetFund);

        assertThat(trades.size(), is(1));

        Trade trade = trades.get(0);
        PortfolioTransaction carriedTransaction = trade.getTransactions().get(0).getTransaction();

        assertThat(carriedTransaction.getType(), is(PortfolioTransaction.Type.FUND_TRANSFER_IN));
        assertThat(carriedTransaction.getDateTime(), is(LocalDateTime.parse("2020-06-01T00:00")));
        assertThat(trade.isClosed(), is(true));
        assertThat(trade.getStart(), is(LocalDateTime.parse("2020-01-01T00:00")));
        assertThat(trade.getHoldingPeriod(), is(366L));
        assertThat(trade.getEntryValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(trade.getEntryValueMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(trade.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(800))));
    }

    @Test
    public void testPartialSaleInLotModePreservesCarriedDateAndCost() throws TradeCollectorException
    {
        var client = new Client();
        var sourceFund = new SecurityBuilder().addTo(client);
        var targetFund = new SecurityBuilder().addTo(client);
        var firstDate = LocalDate.parse("2015-01-02");
        var secondDate = LocalDate.parse("2015-01-05");
        var transferDate = LocalDateTime.parse("2015-01-06T00:00");
        var portfolio = new PortfolioBuilder() //
                        .buy(sourceFund, firstDate.toString(), Values.Share.factorize(1), 10000) //
                        .buy(sourceFund, secondDate.toString(), Values.Share.factorize(3), 30001) //
                        .addTo(client);

        insertFundTransfer(portfolio, portfolio, sourceFund, targetFund, transferDate, Values.Share.factorize(4),
                        Values.Share.factorize(4), 50000,
                        new FundTransferEntry.CarriedLot(firstDate, Values.Share.factorize(1), Values.Share.factorize(1),
                                        Money.of(CurrencyUnit.EUR, 10000), portfolio.getTransactions().get(0).getUUID()),
                        new FundTransferEntry.CarriedLot(secondDate, Values.Share.factorize(3), Values.Share.factorize(3),
                                        Money.of(CurrencyUnit.EUR, 30001), portfolio.getTransactions().get(1).getUUID()));

        var sale = new PortfolioTransaction();
        sale.setType(PortfolioTransaction.Type.SELL);
        sale.setDateTime(LocalDateTime.parse("2015-01-08T00:00"));
        sale.setSecurity(targetFund);
        sale.setShares(Values.Share.factorize(2));
        sale.setMonetaryAmount(Money.of(CurrencyUnit.EUR, 28001));
        portfolio.addTransaction(sale);

        var converter = new TestCurrencyConverter(CurrencyUnit.USD);
        var collector = new TradeCollector(client, converter, TradeGrouping.PER_LOT, new SnapshotCache());
        var trades = collector.collect(targetFund);
        assertThat(trades.size(), is(3));
        assertThat(trades.stream().filter(Trade::isClosed).count(), is(2L));
        assertThat(trades.stream().filter(Trade::isClosed)
                        .mapToLong(trade -> trade.getClosingTransaction().orElseThrow().getTransaction().getAmount()).sum(), is(28001L));

        var closed = trades.stream().filter(Trade::isClosed)
                        .filter(trade -> trade.getStart().equals(secondDate.atStartOfDay())).findFirst().orElseThrow();
        var open = trades.stream().filter(trade -> !trade.isClosed()).findFirst().orElseThrow();
        assertThat(closed.getRealClosingTransaction().orElseThrow(), is(sale));
        assertThat(closed.getHoldingPeriod(), is(3L));
        assertThat(open.getStart(), is(secondDate.atStartOfDay()));

        var closedLot = closed.getTransactions().get(0).getTransaction();
        var openLot = open.getTransactions().get(0).getTransaction();
        assertThat(closedLot.getDateTime(), is(transferDate));
        assertThat(openLot.getDateTime(), is(transferDate));
        assertThat(closedLot.getAmount() + openLot.getAmount(), is(30001L));
        assertThat(closed.getEntryValue(), is(converter.convert(secondDate, closedLot.getMonetaryAmount())));
        assertThat(open.getEntryValue(), is(converter.convert(secondDate, openLot.getMonetaryAmount())));
    }

    private void insertFundTransfer(Portfolio sourcePortfolio, Portfolio targetPortfolio, Security sourceFund,
                    Security targetFund, LocalDateTime date, long sourceShares, long targetShares, long transferAmount,
                    FundTransferEntry.CarriedLot... lots)
    {
        FundTransferEntry entry = new FundTransferEntry(sourcePortfolio, targetPortfolio);
        entry.setDate(date);
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
