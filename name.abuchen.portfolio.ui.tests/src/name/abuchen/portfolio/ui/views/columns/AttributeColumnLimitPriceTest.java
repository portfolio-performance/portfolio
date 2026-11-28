package name.abuchen.portfolio.ui.views.columns;

import static name.abuchen.portfolio.junit.PortfolioBuilder.amountOf;
import static name.abuchen.portfolio.junit.PortfolioBuilder.quoteOf;
import static name.abuchen.portfolio.junit.PortfolioBuilder.sharesOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.LimitPriceConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.model.LimitPrice.RelationalOperator;
import name.abuchen.portfolio.model.LimitPrice.TargetMetric;
import name.abuchen.portfolio.model.LimitPriceResolver;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class AttributeColumnLimitPriceTest
{
    @Test
    public void testComparatorSortsFormulaAndStaticLimitsByNormalizedDistance()
    {
        var today = LocalDate.now();
        var client = new Client();
        var account = new AccountBuilder().addTo(client);

        var limitAttr = new AttributeType("limit-price");
        limitAttr.setName("Limit Price");
        limitAttr.setColumnLabel("LP");
        limitAttr.setTarget(Security.class);
        limitAttr.setType(LimitPrice.class);
        limitAttr.setConverter(LimitPriceConverter.class);
        client.getSettings().addAttributeType(limitAttr);

        // Downside (<): FIFO PP = 100 EUR, limit < 60% PP (60 EUR), latest = 50 EUR -> EXCEEDED (+20% normalized)
        var exceededDownFormula = new SecurityBuilder(CurrencyUnit.EUR)
                        .addPrice(today.toString(), quoteOf(50.00))
                        .addTo(client);
        exceededDownFormula.getAttributes().put(limitAttr,
                        new LimitPrice(RelationalOperator.SMALLER, TargetMetric.PURCHASE_PRICE_FIFO, 0.6d, null));

        // Downside (<): Static limit < 60 EUR, latest = 80 EUR -> NOT exceeded (-25% normalized)
        var notExceededDownStatic = new SecurityBuilder(CurrencyUnit.EUR)
                        .addPrice(today.toString(), quoteOf(80.00))
                        .addTo(client);
        notExceededDownStatic.getAttributes().put(limitAttr,
                        new LimitPrice(RelationalOperator.SMALLER, quoteOf(60.00)));

        // Downside (<): 0 shares held, limit < 60% PP -> unresolved (null distance, sorted first within '<')
        var unresolvedDownZeroShares = new SecurityBuilder(CurrencyUnit.EUR)
                        .addPrice(today.toString(), quoteOf(10.00))
                        .addTo(client);
        unresolvedDownZeroShares.getAttributes().put(limitAttr,
                        new LimitPrice(RelationalOperator.SMALLER, TargetMetric.PURCHASE_PRICE_FIFO, 0.6d, null));

        // Upside (>): FIFO PP = 100 EUR, limit > 150% PP (150 EUR), latest = 200 EUR -> EXCEEDED (+25% normalized)
        var exceededUpFormula = new SecurityBuilder(CurrencyUnit.EUR)
                        .addPrice(today.toString(), quoteOf(200.00))
                        .addTo(client);
        exceededUpFormula.getAttributes().put(limitAttr,
                        new LimitPrice(RelationalOperator.GREATER, TargetMetric.PURCHASE_PRICE_FIFO, 1.5d, null));

        // Upside (>): Static limit > 150 EUR, latest = 120 EUR -> NOT exceeded (-25% normalized)
        var notExceededUpStatic = new SecurityBuilder(CurrencyUnit.EUR)
                        .addPrice(today.toString(), quoteOf(120.00))
                        .addTo(client);
        notExceededUpStatic.getAttributes().put(limitAttr,
                        new LimitPrice(RelationalOperator.GREATER, quoteOf(150.00)));

        // Upside (>): 0 shares held, limit > 150% PP -> unresolved (null distance, sorted first within '>')
        var unresolvedUpZeroShares = new SecurityBuilder(CurrencyUnit.EUR)
                        .addPrice(today.toString(), quoteOf(300.00))
                        .addTo(client);
        unresolvedUpZeroShares.getAttributes().put(limitAttr,
                        new LimitPrice(RelationalOperator.GREATER, TargetMetric.PURCHASE_PRICE_FIFO, 1.5d, null));

        new PortfolioBuilder(account)
                        .inbound_delivery(exceededDownFormula, today.minusDays(10).toString(), sharesOf(10),
                                        amountOf(1000))
                        .inbound_delivery(notExceededDownStatic, today.minusDays(10).toString(), sharesOf(10),
                                        amountOf(1000))
                        .inbound_delivery(exceededUpFormula, today.minusDays(10).toString(), sharesOf(10),
                                        amountOf(1000))
                        .inbound_delivery(notExceededUpStatic, today.minusDays(10).toString(), sharesOf(10),
                                        amountOf(1000))
                        .addTo(client);

        var resolver = new LimitPriceResolver(client, new TestCurrencyConverter(), today);
        var comparator = new AttributeColumn.LimitPriceComparator(limitAttr, resolver);

        // Within '<'
        assertThat(comparator.compare(exceededDownFormula, notExceededDownStatic), greaterThan(0));
        assertThat(comparator.compare(unresolvedDownZeroShares, notExceededDownStatic), lessThan(0));

        // Within '>'
        assertThat(comparator.compare(exceededUpFormula, notExceededUpStatic), greaterThan(0));
        assertThat(comparator.compare(unresolvedUpZeroShares, notExceededUpStatic), lessThan(0));

        // Operator ordering ('<' before '>')
        assertThat(comparator.compare(exceededDownFormula, unresolvedUpZeroShares), lessThan(0));
    }
}
