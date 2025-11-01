package name.abuchen.portfolio.ui.views;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCategory;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.snapshot.trades.TradesGroupedByTaxonomy;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.TouchClientListener;
import name.abuchen.portfolio.ui.util.viewers.MoneyColorLabelProvider;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.trades.TradeElement;

@SuppressWarnings("nls")
public class TradesTableViewerTest
{
    @Test
    public void tradeReturnIsNotWeightedWhenGroupedByTaxonomy() throws Exception
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

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        List<Trade> trades = collector.collect(security);

        Trade trade = trades.get(0);
        TradeElement element = new TradeElement(trade, 0, 0.5);

        double expected = trade.getReturn();

        assertThat(expected, closeTo(0.1, 0.0000001));
        assertThat(TradesTableViewer.getReturnValue(element), closeTo(expected, 0.0000001));
        assertThat(TradesTableViewer.getReturnValue(trade), closeTo(expected, 0.0000001));
    }

    @Test
    public void weightedSharesFollowSecuritySplitRounding() throws Exception
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-02-01", Values.Quote.factorize(110)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2020-01-01", Values.Amount.factorize(20000)) //
                        .addTo(client);

        double tradedShares = 10.0 / 3.0;

        new PortfolioBuilder(account) //
                        .buy(security, "2020-01-01", Values.Share.factorize(tradedShares),
                                        Values.Amount.factorize(tradedShares * 100)) //
                        .sell(security, "2020-02-01", Values.Share.factorize(tradedShares),
                                        Values.Amount.factorize(tradedShares * 110)) //
                        .addTo(client);

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        List<Trade> trades = collector.collect(security);
        Trade trade = trades.get(0);

        int partialWeight = Classification.ONE_HUNDRED_PERCENT / 3;
        double taxonomyWeight = partialWeight / (double) Classification.ONE_HUNDRED_PERCENT;

        TradeElement element = new TradeElement(trade, 0, taxonomyWeight);

        long expectedWeightedShares = BigDecimal.valueOf(trade.getShares()) //
                        .multiply(BigDecimal.valueOf(partialWeight), Values.MC) //
                        .divide(Classification.ONE_HUNDRED_PERCENT_BD, Values.MC) //
                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

        assertThat(element.getWeightedShares(), is(expectedWeightedShares));
        assertThat(new TradeElement(trade, 0, 1.0).getWeightedShares(), is(trade.getShares()));
    }

    @Test
    public void categoryAndTradeRowsShareCurrencyFormattingInSecurityCurrencyMode() throws Exception
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
        TradeCollector collector = new TradeCollector(client, converter.with(CurrencyUnit.USD));

        List<Trade> trades = collector.collect(usdSecurity);
        Trade trade = trades.get(0);

        TradesGroupedByTaxonomy grouped = new TradesGroupedByTaxonomy(taxonomy, trades, converter);
        TradeCategory category = grouped.asList().stream().filter(c -> c.getTaxonomyClassification() == equities)
                        .findFirst().orElse(null);

        TradeElement tradeElement = new TradeElement(trade, 1, 1.0);
        TradeElement categoryElement = new TradeElement(category, 0);

        MoneyColorLabelProvider provider = new MoneyColorLabelProvider(element -> {
            if (element instanceof TradeElement te)
            {
                if (te.isTrade())
                    return te.getTrade().getProfitLoss();
                if (te.isCategory())
                    return te.getCategory().getTotalProfitLoss();
            }
            return null;
        }, client);

        String tradeText = provider.getText(tradeElement);
        String categoryText = provider.getText(categoryElement);

        assertThat(category, notNullValue());
        assertThat(tradeText, notNullValue());
        assertThat(categoryText, notNullValue());
        assertThat(tradeText, is(categoryText));
        assertThat(tradeText, containsString(CurrencyUnit.USD));
    }

    @Test
    public void renamingSecurityTouchesClient() throws Exception
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-02-01", Values.Quote.factorize(110)) //
                        .addTo(client);
        security.setName("Original");

        Account account = new AccountBuilder() //
                        .deposit_("2020-01-01", Values.Amount.factorize(20000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2020-01-01", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(security, "2020-02-01", Values.Share.factorize(100), Values.Amount.factorize(11000)) //
                        .addTo(client);

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        Trade trade = collector.collect(security).get(0);

        AtomicBoolean touched = new AtomicBoolean(false);
        client.addPropertyChangeListener("touch", event -> touched.set(true));

        Column column = new NameColumn(client);
        column.getEditingSupport().addListener(new TouchClientListener(client));

        column.getEditingSupport().setValue(trade, "Renamed Security");

        assertThat(security.getName(), is("Renamed Security"));
        assertThat(client.getSecurities().get(0).getName(), is("Renamed Security"));
        assertThat(touched.get(), is(true));
    }

    @Test
    public void renamingSecurityRefreshesAllTradeElements() throws Exception
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2020-02-01", Values.Quote.factorize(110)) //
                        .addTo(client);
        security.setName("Original");

        Account account = new AccountBuilder() //
                        .deposit_("2020-01-01", Values.Amount.factorize(20000)) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2020-01-01", Values.Share.factorize(100), Values.Amount.factorize(10000)) //
                        .sell(security, "2020-02-01", Values.Share.factorize(100), Values.Amount.factorize(11000)) //
                        .addTo(client);

        TradeCollector collector = new TradeCollector(client, new TestCurrencyConverter());
        Trade trade = collector.collect(security).get(0);

        // Create column directly without needing the full viewer infrastructure
        Column column = new NameColumn(client);

        // Track whether the refresh listener is called (simulating viewer
        // refresh)
        AtomicBoolean refreshCalled = new AtomicBoolean(false);
        column.getEditingSupport().addListener((element, newValue, oldValue) -> refreshCalled.set(true));

        TradeElement element = new TradeElement(trade, 0, 1.0);

        // Execute the edit
        column.getEditingSupport().setValue(element, "Renamed Security");

        // Verify the security was renamed and the refresh listener was
        // triggered
        assertThat(security.getName(), is("Renamed Security"));
        assertThat(refreshCalled.get(), is(true));
    }
}
