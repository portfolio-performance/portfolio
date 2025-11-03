package name.abuchen.portfolio.snapshot.trades;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
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

        new PortfolioBuilder(account) //
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
        TradesGroupedByTaxonomy grouped = new TradesGroupedByTaxonomy(taxonomy, allTrades, new TestCurrencyConverter());

        assertThat(grouped.asList().size(), is(2));

        TradeCategory stocksCategory = grouped.byClassification(stocks);
        assertThat(stocksCategory, notNullValue());
        assertThat(stocksCategory.getTradeCount(), is(1L));
        assertThat(stocksCategory.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));

        TradeCategory bondsCategory = grouped.byClassification(bonds);
        assertThat(bondsCategory, notNullValue());
        assertThat(bondsCategory.getTradeCount(), is(1L));
        assertThat(bondsCategory.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));

        // verify total
        assertThat(grouped.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500))));
    }

    @Test
    public void testGroupingIncludesChildClassifications() throws Exception
    {
        Client client = new Client();

        Security parentSecurity = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-02-01", Values.Quote.factorize(110)) //
                        .addTo(client);

        Security childSecurity = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-02-01", Values.Quote.factorize(104)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2020-01-01", Values.Amount.factorize(50000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(parentSecurity, "2020-01-01", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(parentSecurity, "2020-02-01", Values.Share.factorize(100), Values.Amount.factorize(11000)) //
                        .buy(childSecurity, "2020-01-01", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(childSecurity, "2020-02-01", Values.Share.factorize(100), Values.Amount.factorize(10400)) //
                        .addTo(client);

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("equities") //
                        .addClassification("equities", "growth") //
                        .addTo(client);

        Classification equities = taxonomy.getClassificationById("equities");
        Classification growth = taxonomy.getClassificationById("growth");

        equities.addAssignment(new Classification.Assignment(parentSecurity));
        growth.addAssignment(new Classification.Assignment(childSecurity));

        List<Trade> trades = new java.util.ArrayList<>();
        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());

        trades.addAll(collector.collect(parentSecurity));
        trades.addAll(collector.collect(childSecurity));

        TradesGroupedByTaxonomy grouped = new TradesGroupedByTaxonomy(taxonomy, trades, new TestCurrencyConverter());

        TradeCategory equitiesCategory = grouped.byClassification(equities);
        assertThat(equitiesCategory, notNullValue());
        assertThat(equitiesCategory.getTotalProfitLoss(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));

        TradeCategory growthCategory = grouped.byClassification(growth);
        assertThat(growthCategory, notNullValue());
        assertThat(growthCategory.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(400))));

        assertThat(grouped.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1400))));
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

        new PortfolioBuilder(account) //
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

        String techCurrency = techCategory.getTotalProfitLoss().getCurrencyCode();
        Money techContribution = techCategory.getTradeAssignments().stream() //
                        .map(a -> a.getTrade().getProfitLoss().multiplyAndRound(a.getWeight())) //
                        .collect(MoneyCollectors.sum(techCurrency));
        assertThat(techContribution, is(techCategory.getTotalProfitLoss()));

        String healthcareCurrency = healthcareCategory.getTotalProfitLoss().getCurrencyCode();
        Money healthcareContribution = healthcareCategory.getTradeAssignments().stream() //
                        .map(a -> a.getTrade().getProfitLoss().multiplyAndRound(a.getWeight())) //
                        .collect(MoneyCollectors.sum(healthcareCurrency));
        assertThat(healthcareContribution, is(healthcareCategory.getTotalProfitLoss()));

        // total should still be 1000
        assertThat(grouped.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
    }

    @Test
    public void testPartialAssignmentBelowHalfKeepsCategoryAndUnassigned() throws Exception
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
                        .addClassification("tech") //
                        .addTo(client);

        Classification tech = taxonomy.getClassificationById("tech");

        // assign 25% to tech, leaving 75% unassigned
        tech.addAssignment(new Classification.Assignment(security, Classification.ONE_HUNDRED_PERCENT / 4));

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        var trades = collector.collect(security);

        TradesGroupedByTaxonomy grouped = new TradesGroupedByTaxonomy(taxonomy, trades, new TestCurrencyConverter());

        assertThat(grouped.asList().size(), is(2));

        TradeCategory techCategory = grouped.byClassification(tech);
        assertThat(techCategory, notNullValue());
        assertThat(techCategory.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(250))));

        TradeCategory unassignedCategory = grouped.asList().stream() //
                        .filter(c -> Classification.UNASSIGNED_ID.equals(c.getClassification().getId())) //
                        .findFirst().orElse(null);

        assertThat(unassignedCategory, notNullValue());
        assertThat(unassignedCategory.getTotalProfitLoss(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(750))));

        // total should still be 1000
        assertThat(grouped.getTotalProfitLoss(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
    }

    @Test
    public void testTotalIRRRemainsPositiveWithOutOfOrderClosingDates() throws Exception
    {
        Client client = new Client();

        Security longHold = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-03-10", Values.Quote.factorize(110)) //
                        .addTo(client);

        Security quickFlip = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-01-20", Values.Quote.factorize(110)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2020-01-01", Values.Amount.factorize(30000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(longHold, "2020-01-10", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(longHold, "2020-03-10", Values.Share.factorize(100), Values.Amount.factorize(11000)) //
                        .buy(quickFlip, "2020-01-05", Values.Share.factorize(50), Values.Amount.factorize(5000)) //
                        .sell(quickFlip, "2020-01-20", Values.Share.factorize(50), Values.Amount.factorize(5500)) //
                        .addTo(client);

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("equities") //
                        .addTo(client);

        Classification equities = taxonomy.getClassificationById("equities");
        equities.addAssignment(new Classification.Assignment(longHold));
        equities.addAssignment(new Classification.Assignment(quickFlip));

        TestCurrencyConverter converter = new TestCurrencyConverter();
        TradeCollector collector = new TradeCollector(client, converter);

        List<Trade> trades = new java.util.ArrayList<>();
        trades.addAll(collector.collect(longHold));
        trades.addAll(collector.collect(quickFlip));

        TradesGroupedByTaxonomy grouped = new TradesGroupedByTaxonomy(taxonomy, trades, converter);

        Classification totalClassification = new Classification(null, "total", "Total");
        TradeCategory totals = new TradeCategory(totalClassification, converter);
        for (Trade trade : grouped.getTrades())
        {
            totals.addTrade(trade, 1.0);
        }

        assertThat(totals.getTotalProfitLoss().getAmount(), greaterThan(0L));
        assertThat(totals.getAverageIRR(), greaterThan(0.0));
    }

    @Test
    public void testSecurityCurrencyModeWithSingleCurrencyKeepsCategoryCurrency() throws Exception
    {
        Client client = new Client();
        client.setBaseCurrency(CurrencyUnit.EUR);

        Security usdSecurity = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice("2015-01-02", Values.Quote.factorize(100)) //
                        .addPrice("2015-01-09", Values.Quote.factorize(110)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2015-01-01", Values.Amount.factorize(200000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(usdSecurity, "2015-01-02", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(usdSecurity, "2015-01-09", Values.Share.factorize(100), Values.Amount.factorize(11000)) //
                        .addTo(client);

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("equities") //
                        .addTo(client);

        Classification equities = taxonomy.getClassificationById("equities");
        equities.addAssignment(new Classification.Assignment(usdSecurity));

        TestCurrencyConverter converter = new TestCurrencyConverter(CurrencyUnit.EUR);

        List<Trade> trades = new java.util.ArrayList<>();
        trades.addAll(new TradeCollector(client, converter.with(CurrencyUnit.USD)).collect(usdSecurity));

        TradesGroupedByTaxonomy grouped = new TradesGroupedByTaxonomy(taxonomy, trades, converter);

        TradeCategory equitiesCategory = grouped.byClassification(equities);
        assertThat(equitiesCategory, notNullValue());
        assertThat(equitiesCategory.getCurrencyKey(), is(CurrencyUnit.USD));

        Trade usdTrade = trades.get(0);
        assertThat(usdTrade.getProfitLoss().getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(equitiesCategory.getTotalProfitLoss(), is(usdTrade.getProfitLoss()));
        assertThat(grouped.asList().size(), is(1));
    }

    @Test
    public void testTotalProfitLossConvertedFromSecurityCurrencies() throws Exception
    {
        Client client = new Client();

        Security eurSecurity = new SecurityBuilder(CurrencyUnit.EUR) //
                        .addPrice("2015-01-02", Values.Quote.factorize(100)) //
                        .addPrice("2015-01-09", Values.Quote.factorize(110)) //
                        .addTo(client);

        Security usdSecurity = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice("2015-01-02", Values.Quote.factorize(100)) //
                        .addPrice("2015-01-09", Values.Quote.factorize(110)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2015-01-01", Values.Amount.factorize(200000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(eurSecurity, "2015-01-02", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(eurSecurity, "2015-01-09", Values.Share.factorize(100), Values.Amount.factorize(11000)) //
                        .buy(usdSecurity, "2015-01-02", Values.Share.factorize(50), Values.Amount.factorize(8000)) //
                        .sell(usdSecurity, "2015-01-09", Values.Share.factorize(50), Values.Amount.factorize(8500)) //
                        .addTo(client);

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("equities") //
                        .addTo(client);

        Classification equities = taxonomy.getClassificationById("equities");
        equities.addAssignment(new Classification.Assignment(eurSecurity));
        equities.addAssignment(new Classification.Assignment(usdSecurity));

        TestCurrencyConverter converter = new TestCurrencyConverter(CurrencyUnit.EUR);

        List<Trade> trades = new java.util.ArrayList<>();
        trades.addAll(new TradeCollector(client, converter.with(CurrencyUnit.EUR)).collect(eurSecurity));
        trades.addAll(new TradeCollector(client, converter.with(CurrencyUnit.USD)).collect(usdSecurity));

        TradesGroupedByTaxonomy grouped = new TradesGroupedByTaxonomy(taxonomy, trades, converter);

        TradeCategory equitiesCategory = grouped.byClassification(equities);
        assertThat(equitiesCategory, notNullValue());
        assertThat(equitiesCategory.getTaxonomyClassification(), is(equities));

        Money expectedTotal = trades.stream() //
                        .map(trade -> {
                            LocalDate date = trade.getEnd().map(LocalDate::from).orElse(LocalDate.now());
                            return trade.getProfitLoss().with(converter.at(date));
                        }) //
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));

        assertThat(grouped.getTotalProfitLoss(), is(expectedTotal));

        Money expectedWithout = trades.stream() //
                        .map(trade -> {
                            LocalDate date = trade.getEnd().map(LocalDate::from).orElse(LocalDate.now());
                            return trade.getProfitLossWithoutTaxesAndFees().with(converter.at(date));
                        }) //
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));

        assertThat(grouped.getTotalProfitLossWithoutTaxesAndFees(), is(expectedWithout));
    }

    @Test
    public void testMultiCurrencyCloneOrderingIsStable() throws Exception
    {
        var observedOrders = new java.util.HashSet<List<String>>();

        observedOrders.add(extractCurrencyOrder(false));
        observedOrders.add(extractCurrencyOrder(true));
        observedOrders.add(extractCurrencyOrder(false));

        assertThat(observedOrders.size(), is(1));
        assertThat(observedOrders.iterator().next(), is(List.of(CurrencyUnit.EUR, CurrencyUnit.USD)));
    }

    private List<String> extractCurrencyOrder(boolean reverseTrades) throws Exception
    {
        List<TradeCategory> categories = buildMultiCurrencyCategories(reverseTrades);
        assertThat(categories.size(), is(2));
        return categories.stream().map(TradeCategory::getCurrencyKey).toList();
    }

    private List<TradeCategory> buildMultiCurrencyCategories(boolean reverseTrades) throws Exception
    {
        Client client = new Client();

        Security eurSecurity = new SecurityBuilder(CurrencyUnit.EUR) //
                        .addPrice("2015-01-02", Values.Quote.factorize(100)) //
                        .addPrice("2015-01-09", Values.Quote.factorize(110)) //
                        .addTo(client);

        Security usdSecurity = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice("2015-01-02", Values.Quote.factorize(100)) //
                        .addPrice("2015-01-09", Values.Quote.factorize(110)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2015-01-01", Values.Amount.factorize(200000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(eurSecurity, "2015-01-02", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(eurSecurity, "2015-01-09", Values.Share.factorize(100), Values.Amount.factorize(11000)) //
                        .buy(usdSecurity, "2015-01-02", Values.Share.factorize(50), Values.Amount.factorize(8000)) //
                        .sell(usdSecurity, "2015-01-09", Values.Share.factorize(50), Values.Amount.factorize(8500)) //
                        .addTo(client);

        Taxonomy taxonomy = new TaxonomyBuilder() //
                        .addClassification("global") //
                        .addTo(client);

        Classification global = taxonomy.getClassificationById("global");
        global.addAssignment(new Classification.Assignment(eurSecurity));
        global.addAssignment(new Classification.Assignment(usdSecurity));

        TestCurrencyConverter converter = new TestCurrencyConverter(CurrencyUnit.EUR);

        List<Trade> trades = new java.util.ArrayList<>();
        trades.addAll(new TradeCollector(client, converter.with(CurrencyUnit.EUR)).collect(eurSecurity));
        trades.addAll(new TradeCollector(client, converter.with(CurrencyUnit.USD)).collect(usdSecurity));

        if (reverseTrades)
            java.util.Collections.reverse(trades);

        TradesGroupedByTaxonomy grouped = new TradesGroupedByTaxonomy(taxonomy, trades, converter);

        return grouped.asList().stream() //
                        .filter(c -> c.getTaxonomyClassification() == global) //
                        .toList();
    }
}
