package name.abuchen.portfolio.snapshot.trades;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
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

        Portfolio portfolio = new PortfolioBuilder(account) //
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
        assertThat(category.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(category.getWinningTradesCount(), is(1L));
        assertThat(category.getLosingTradesCount(), is(0L));
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

        Portfolio portfolio = new PortfolioBuilder(account) //
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
        assertThat(category.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));
    }
}
