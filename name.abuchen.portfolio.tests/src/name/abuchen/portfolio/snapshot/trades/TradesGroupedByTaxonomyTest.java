package name.abuchen.portfolio.snapshot.trades;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

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
public class TradesGroupedByTaxonomyTest
{
    @Test
    public void testGrouping() throws Exception
    {
        Client client = new Client();

        Security stockA = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-02-01", Values.Quote.factorize(110)) //
                        .addTo(client);

        Security bondB = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-02-01", Values.Quote.factorize(105)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2020-01-01", Values.Amount.factorize(50000)) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder(account) //
                        .buy(stockA, "2020-01-01", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(stockA, "2020-02-01", Values.Share.factorize(100), Values.Amount.factorize(11000)) //
                        .buy(bondB, "2020-01-01", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(bondB, "2020-02-01", Values.Share.factorize(100), Values.Amount.factorize(10500)) //
                        .addTo(client);

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("stocks") //
                        .addClassification("bonds") //
                        .addTo(client);

        Classification stocks = taxonomy.getClassificationById("stocks");
        Classification bonds = taxonomy.getClassificationById("bonds");

        stocks.addAssignment(new Classification.Assignment(stockA));
        bonds.addAssignment(new Classification.Assignment(bondB));

        // collect all trades
        List<Trade> allTrades = new java.util.ArrayList<>();
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        allTrades.addAll(collector.collect(stockA));
        allTrades.addAll(collector.collect(bondB));

        assertThat(allTrades.size(), is(2));

        // group by taxonomy
        TradesGroupedByTaxonomy grouped = new TradesGroupedByTaxonomy(taxonomy, allTrades,
                        new TestCurrencyConverter());

        assertThat(grouped.asList().size(), is(2));

        TradeCategory stocksCategory = grouped.byClassification(stocks);
        assertThat(stocksCategory, notNullValue());
        assertThat(stocksCategory.getTradeCount(), is(1L));
        assertThat(stocksCategory.getTotalProfitLoss(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));

        TradeCategory bondsCategory = grouped.byClassification(bonds);
        assertThat(bondsCategory, notNullValue());
        assertThat(bondsCategory.getTradeCount(), is(1L));
        assertThat(bondsCategory.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));

        // verify total
        assertThat(grouped.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500))));
    }

    @Test
    public void testPartialAssignment() throws Exception
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
                        .addClassification("tech") //
                        .addClassification("healthcare") //
                        .addTo(client);

        Classification tech = taxonomy.getClassificationById("tech");
        Classification healthcare = taxonomy.getClassificationById("healthcare");

        // assign 50% to tech, 50% to healthcare
        tech.addAssignment(new Classification.Assignment(security, Classification.ONE_HUNDRED_PERCENT / 2));
        healthcare.addAssignment(new Classification.Assignment(security, Classification.ONE_HUNDRED_PERCENT / 2));

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        var trades = collector.collect(security);

        TradesGroupedByTaxonomy grouped = new TradesGroupedByTaxonomy(taxonomy, trades, new TestCurrencyConverter());

        // verify both categories have half the profit
        TradeCategory techCategory = grouped.byClassification(tech);
        assertThat(techCategory.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));

        TradeCategory healthcareCategory = grouped.byClassification(healthcare);
        assertThat(healthcareCategory.getTotalProfitLoss(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));

        // total should still be 1000
        assertThat(grouped.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
    }
}
