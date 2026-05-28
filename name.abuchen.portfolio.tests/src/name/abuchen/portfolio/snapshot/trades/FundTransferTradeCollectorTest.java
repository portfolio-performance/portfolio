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
        insertFundTransfer(sourcePortfolio, targetPortfolio, sourceFund, targetFund, Values.Share.factorize(10),
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
        assertThat(trade.isClosed(), is(true));
        assertThat(trade.getStart(), is(LocalDateTime.parse("2020-01-01T00:00")));
        assertThat(trade.getHoldingPeriod(), is(366L));
        assertThat(trade.getEntryValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(trade.getEntryValueMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(trade.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(800))));
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
