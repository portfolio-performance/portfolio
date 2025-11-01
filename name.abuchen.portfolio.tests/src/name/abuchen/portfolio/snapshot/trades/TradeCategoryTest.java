package name.abuchen.portfolio.snapshot.trades;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TradeCategoryTest
{
    @Test
    public void testAggregation() throws Exception
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-02-01", Values.Quote.factorize(110)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2020-01-01", Values.Amount.factorize(20000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2020-01-01", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(security, "2020-02-01", Values.Share.factorize(100), Values.Amount.factorize(11000)) //
                        .addTo(client);

        // create a simple taxonomy
        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("stocks") //
                        .addTo(client);

        Classification stocks = taxonomy.getClassificationById("stocks");
        stocks.addAssignment(new Classification.Assignment(security));

        // collect trades
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        var trades = collector.collect(security);

        assertThat(trades.size(), is(1));

        // create category and add trades
        TradeCategory category = new TradeCategory(stocks, new TestCurrencyConverter());
        category.addTrade(trades.get(0), 1.0);

        // verify aggregations
        assertThat(category.getTradeCount(), is(1L));
        assertThat(category.getTotalEntryValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10000))));
        assertThat(category.getTotalExitValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11000))));
        assertThat(category.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(category.getTotalProfitLossMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(category.getTotalProfitLossMovingAverageWithoutTaxesAndFees(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(category.getWinningTradesCount(), is(1L));
        assertThat(category.getLosingTradesCount(), is(0L));
        assertThat(category.getAverageReturnMovingAverage(), is(closeTo(0.1, 1e-10)));
    }

    @Test
    public void testWeightedAggregation() throws Exception
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-02-01", Values.Quote.factorize(110)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2020-01-01", Values.Amount.factorize(20000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2020-01-01", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(security, "2020-02-01", Values.Share.factorize(100), Values.Amount.factorize(11000)) //
                        .addTo(client);

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("stocks") //
                        .addTo(client);

        Classification stocks = taxonomy.getClassificationById("stocks");

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        var trades = collector.collect(security);

        assertThat(trades.size(), is(1));

        // add trade with 50% weight
        TradeCategory category = new TradeCategory(stocks, new TestCurrencyConverter());
        category.addTrade(trades.get(0), 0.5);

        // verify weighted aggregations - profit should be 50% of 1000 = 500
        assertThat(category.getTotalEntryValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000))));
        assertThat(category.getTotalExitValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5500))));
        assertThat(category.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));
        assertThat(category.getTotalProfitLossMovingAverage(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));
        assertThat(category.getTotalProfitLossMovingAverageWithoutTaxesAndFees(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));
        assertThat(category.getAverageReturnMovingAverage(), is(closeTo(0.1, 1e-10)));

        assertThat(category.getTradeAssignments().size(), is(1));
        TradeCategory.TradeAssignment assignment = category.getTradeAssignments().get(0);
        assertThat(assignment.getTrade(), is(trades.get(0)));
        assertThat(assignment.getWeight(), is(0.5));
    }

    @Test
    public void testPartialWeightedTradeCounts() throws Exception
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-02-01", Values.Quote.factorize(110)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2020-01-01", Values.Amount.factorize(20000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2020-01-01", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(security, "2020-02-01", Values.Share.factorize(100), Values.Amount.factorize(11000)) //
                        .addTo(client);

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("stocks") //
                        .addTo(client);

        Classification stocks = taxonomy.getClassificationById("stocks");

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        var trades = collector.collect(security);

        TradeCategory category = new TradeCategory(stocks, new TestCurrencyConverter());
        category.addTrade(trades.get(0), 0.4);

        assertThat(category.getTradeCount(), is(1L));
        assertThat(category.getWinningTradesCount(), is(1L));
        assertThat(category.getLosingTradesCount(), is(0L));
    }

    @Test
    public void testAverageReturnForProfitableShortTrade() throws Exception
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-02-01", Values.Quote.factorize(90)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2020-01-01", Values.Amount.factorize(20000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .sell(security, "2020-01-01", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .buy(security, "2020-02-01", Values.Share.factorize(100), Values.Amount.factorize(9000)) //
                        .addTo(client);

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("shorts") //
                        .addTo(client);

        Classification shorts = taxonomy.getClassificationById("shorts");
        shorts.addAssignment(new Classification.Assignment(security));

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        var trades = collector.collect(security);

        assertThat(trades.size(), is(1));

        Trade shortTrade = trades.get(0);
        assertThat(shortTrade.isLong(), is(false));
        assertThat(shortTrade.getProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));

        TradeCategory category = new TradeCategory(shorts, new TestCurrencyConverter());
        category.addTrade(shortTrade, 1.0);

        assertThat(category.getAverageReturn(), is(closeTo(shortTrade.getReturn(), 1e-10)));
    }

    @Test
    public void testCategoryIRRInSecurityCurrencyModeForOpenTrade() throws Exception
    {
        LocalDate tradeDate = LocalDate.of(2020, 1, 8);
        LocalDate valuationDate = LocalDate.now();

        Client client = new Client();

        Security security = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice(tradeDate.toString(), Values.Quote.factorize(100)) //
                        .addPrice(valuationDate.toString(), Values.Quote.factorize(120)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_(tradeDate.toString(), Values.Amount.factorize(20000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, tradeDate.toString(), Values.Share.factorize(100),
                                        Values.Amount.factorize(10000)) //
                        .addTo(client);

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("stocks") //
                        .addTo(client);

        Classification stocks = taxonomy.getClassificationById("stocks");
        stocks.addAssignment(new Classification.Assignment(security));

        TestCurrencyConverter converter = new TestCurrencyConverter();
        TradeCollector collector = new TradeCollector(client, converter);
        var trades = collector.collect(security);

        assertThat(trades.size(), is(1));

        Trade trade = trades.get(0);
        assertThat(trade.isClosed(), is(false));

        TradeCategory category = new TradeCategory(stocks, converter, security.getCurrencyCode());
        category.addTrade(trade, 1.0);

        double aggregatedIrr = category.getAverageIRR();

        var securityCurrencyConverter = Objects.requireNonNull(converter.with(security.getCurrencyCode()));
        List<LocalDate> dates = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        double collateral = 0;

        for (TransactionPair<PortfolioTransaction> txPair : trade.getTransactions())
        {
            LocalDate date = txPair.getTransaction().getDateTime().toLocalDate();
            double amount = txPair.getTransaction().getMonetaryAmount()
                            .with(securityCurrencyConverter.at(txPair.getTransaction().getDateTime())).getAmount()
                            / Values.Amount.divider();

            if (txPair.getTransaction().getType().isPurchase() == trade.isLong())
            {
                collateral += amount;
                amount = -amount;
            }
            else if (!trade.isLong())
            {
                amount = collateral - amount;
            }

            dates.add(date);
            values.add(amount);
        }

        double exitAmount = trade.getExitValue().with(securityCurrencyConverter.at(valuationDate)).getAmount()
                        / Values.Amount.divider();
        dates.add(valuationDate);
        if (!trade.isLong())
            exitAmount = collateral - exitAmount;
        values.add(exitAmount);

        if (!trade.isLong())
        {
            dates.add(valuationDate);
            values.add(collateral);
        }

        double expectedIrr = IRR.calculate(dates, values);

        assertThat(aggregatedIrr, is(closeTo(expectedIrr, 1e-10)));
    }

    @Test
    public void testCategoryIRRForShortTradeWithMultipleCoveringBuys() throws Exception
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2024-01-01", Values.Quote.factorize(10)) //
                        .addPrice("2024-03-01", Values.Quote.factorize(8)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2024-01-01", Values.Amount.factorize(20000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .sellPrice(security, "2024-01-01", 100.0, 10.0) // Sell
                                                                        // 100
                        .buyPrice(security, "2024-02-01", 40.0, 9.0) // Cover 40
                        .buyPrice(security, "2024-03-01", 60.0, 8.0) // Cover 60
                        .addTo(client);

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("shorts") //
                        .addTo(client);

        Classification shorts = taxonomy.getClassificationById("shorts");
        shorts.addAssignment(new Classification.Assignment(security));

        TestCurrencyConverter converter = new TestCurrencyConverter();
        TradeCollector collector = new TradeCollector(client, converter);
        var trades = collector.collect(security);

        assertThat(trades.size(), is(2));

        Trade trade1 = trades.get(0);
        Trade trade2 = trades.get(1);

        TradeCategory category = new TradeCategory(shorts, converter);
        category.addTrade(trade1, 1.0);
        category.addTrade(trade2, 1.0);

        List<LocalDate> dates = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        dates.add(LocalDate.of(2024, 1, 1));
        values.add(-400.0);
        dates.add(LocalDate.of(2024, 2, 1));
        values.add(40.0);
        dates.add(LocalDate.of(2024, 2, 1));
        values.add(400.0);

        dates.add(LocalDate.of(2024, 1, 1));
        values.add(-600.0);
        dates.add(LocalDate.of(2024, 3, 1));
        values.add(120.0);
        dates.add(LocalDate.of(2024, 3, 1));
        values.add(600.0);

        double expectedIrr = IRR.calculate(dates, values);

        assertThat(category.getAverageIRR(), is(closeTo(expectedIrr, 1e-10)));
    }
}
