package name.abuchen.portfolio.model;

import static name.abuchen.portfolio.junit.PortfolioBuilder.amountOf;
import static name.abuchen.portfolio.junit.PortfolioBuilder.quoteOf;
import static name.abuchen.portfolio.junit.PortfolioBuilder.sharesOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.OptionalLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.AttributeType.BooleanConverter;
import name.abuchen.portfolio.model.LimitPrice.RelationalOperator;
import name.abuchen.portfolio.model.LimitPrice.TargetMetric;
import name.abuchen.portfolio.model.LimitPriceResolver.Status;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.snapshot.filter.ClientSecurityFilter;

@SuppressWarnings("nls")
public class LimitPriceResolverTest
{
    private static final LocalDate AS_OF = LocalDate.parse("2015-01-16");

    private Locale defaultLocale;

    @Before
    public void setupLocale()
    {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
    }

    @After
    public void restoreLocale()
    {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testMultiLotFifoPurchasePriceAcrossPortfolios()
    {
        var client = new Client();
        var account = new AccountBuilder().addTo(client);

        var ags = new SecurityBuilder(CurrencyUnit.EUR)
                        .addPrice("2015-01-16", quoteOf(32.00))
                        .addTo(client);

        // Lot 1 (Portfolio 1): Buy 10 shares @ 40 EUR net + 5 EUR fees (gross 405 EUR) -> sold 5 shares later
        // Remaining from Lot 1: 5 shares @ 40 EUR = 200 EUR net
        new PortfolioBuilder(account)
                        .inbound_delivery(ags, "2015-01-02", sharesOf(10), amountOf(405), amountOf(5), 0L)
                        .outbound_delivery(ags, "2015-01-10", sharesOf(5), amountOf(250), 0L, 0L)
                        .addTo(client);

        // Lot 2 (Portfolio 2): Buy 5 shares @ 60 EUR net + 5 EUR fees (gross 305 EUR) -> 300 EUR net
        // Combined held across both portfolios: 10 shares, net FIFO cost = 500 EUR -> 50.00 EUR / share
        new PortfolioBuilder(account)
                        .inbound_delivery(ags, "2015-01-05", sharesOf(5), amountOf(305), amountOf(5), 0L)
                        .addTo(client);

        var resolver = new LimitPriceResolver(client, new TestCurrencyConverter(), AS_OF);

        var downLimit = new LimitPrice(RelationalOperator.SMALLER_OR_EQUAL,
                        TargetMetric.PURCHASE_PRICE_FIFO, 0.6d, null);
        var upLimit = new LimitPrice(RelationalOperator.GREATER,
                        TargetMetric.PURCHASE_PRICE_FIFO, 1.8d, null);

        // 60% of 50.00 EUR = 30.00 EUR; 180% of 50.00 EUR = 90.00 EUR
        var downRes = resolver.resolve(downLimit, ags);
        assertThat(downRes.status(), is(Status.RESOLVED));
        assertThat(downRes.fifoPurchaseQuote(), is(OptionalLong.of(quoteOf(50.00))));
        assertThat(downRes.resolvedQuote(), is(OptionalLong.of(quoteOf(30.00))));

        var upResolved = upLimit.resolveValue(resolver, ags);
        assertThat(upResolved, is(OptionalLong.of(quoteOf(90.00))));

        // Downside (<= 30.00): 32.00 -> false, 30.00 -> true, 29.50 -> true
        assertThat(downLimit.isExceeded(new SecurityPrice(AS_OF, quoteOf(32.00)), resolver, ags), is(false));
        assertThat(downLimit.isExceeded(new SecurityPrice(AS_OF, quoteOf(30.00)), resolver, ags), is(true));
        assertThat(downLimit.isExceeded(new SecurityPrice(AS_OF, quoteOf(29.50)), resolver, ags), is(true));

        // Strict upside (> 90.00): 90.00 -> false, 90.01 -> true
        assertThat(upLimit.isExceeded(new SecurityPrice(AS_OF, quoteOf(90.00)), resolver, ags), is(false));
        assertThat(upLimit.isExceeded(new SecurityPrice(AS_OF, quoteOf(90.01)), resolver, ags), is(true));

        // Check formatting via LimitPriceSettings
        var settings = new LimitPriceSettings(new TypedMap());
        assertThat(settings.getFullLabel(downLimit, downRes.resolvedQuote(), new SecurityPrice(AS_OF, quoteOf(32.00))),
                        is("<= 60% PP (30,00)"));
        settings.setShowRelativeDiff(true);
        assertThat(settings.getFullLabel(downLimit, downRes.resolvedQuote(), new SecurityPrice(AS_OF, quoteOf(40.00))),
                        is("<= 60% PP: 30,00 (-25%)"));
        assertThat(settings.getFullLabel(upLimit, upResolved, new SecurityPrice(AS_OF, quoteOf(50.00))),
                        is("> 180% PP: 90,00 (+80%)"));
    }

    @Test
    public void testAvgPositionValueMultiCurrencyAndCheckboxFilters()
    {
        var client = new Client();
        client.setBaseCurrency(CurrencyUnit.EUR);
        var account = new AccountBuilder().addTo(client);

        // Create Boolean Checkbox Security Attribute with distinct Name and Column Label
        var coreCheckbox = new AttributeType("attr-core");
        coreCheckbox.setName("Core Holdings");
        coreCheckbox.setColumnLabel("Core");
        coreCheckbox.setTarget(Security.class);
        coreCheckbox.setType(Boolean.class);
        coreCheckbox.setConverter(BooleanConverter.class);
        client.getSettings().addAttributeType(coreCheckbox);

        var emptyCheckbox = new AttributeType("attr-empty");
        emptyCheckbox.setName("Satellite");
        emptyCheckbox.setColumnLabel("Sat");
        emptyCheckbox.setTarget(Security.class);
        emptyCheckbox.setType(Boolean.class);
        emptyCheckbox.setConverter(BooleanConverter.class);
        client.getSettings().addAttributeType(emptyCheckbox);

        // Security A (EUR, Core=true): 10 shares @ 100 EUR = 1,000.00 EUR
        var secEurCore = new SecurityBuilder(CurrencyUnit.EUR)
                        .addPrice("2015-01-16", quoteOf(100.00))
                        .addTo(client);
        secEurCore.getAttributes().put(coreCheckbox, Boolean.TRUE);

        // Security B (USD, Core=true): 20 shares @ 115.88 USD = 2,317.60 USD
        // On 2015-01-16 TestCurrencyConverter EUR/USD = 1.1588 -> 2,317.60 USD = 2,000.00 EUR
        var secUsdCore = new SecurityBuilder(CurrencyUnit.USD)
                        .addPrice("2015-01-16", quoteOf(115.88))
                        .addTo(client);
        secUsdCore.getAttributes().put(coreCheckbox, Boolean.TRUE);

        // Security C (EUR, Core=false): 30 shares @ 100 EUR = 3,000.00 EUR
        var secEurNonCore = new SecurityBuilder(CurrencyUnit.EUR)
                        .addPrice("2015-01-16", quoteOf(100.00))
                        .addTo(client);
        secEurNonCore.getAttributes().put(coreCheckbox, Boolean.FALSE);

        // Security D (EUR, Core=true, 0 shares held / watchlist): must be excluded from average AND inactive!
        var secWatchlist = new SecurityBuilder(CurrencyUnit.EUR)
                        .addPrice("2015-01-16", quoteOf(50.00))
                        .addTo(client);
        secWatchlist.getAttributes().put(coreCheckbox, Boolean.TRUE);

        var portfolio = new PortfolioBuilder(account)
                        .inbound_delivery(secEurCore, "2015-01-02", sharesOf(10), amountOf(1000))
                        .outbound_delivery(secWatchlist, "2015-01-10", sharesOf(5), amountOf(250), 0L, 0L)
                        .inbound_delivery(secWatchlist, "2015-01-02", sharesOf(5), amountOf(250))
                        .inbound_delivery(secEurNonCore, "2015-01-02", sharesOf(30), amountOf(3000))
                        .addTo(client);

        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.parse("2015-01-02T00:00"), CurrencyUnit.USD,
                        amountOf(2317.60), secUsdCore, sharesOf(20), PortfolioTransaction.Type.DELIVERY_INBOUND, 0L,
                        0L));

        // Also verify ReadOnlyClient unwrapping works seamlessly
        var wrapped = new ClientSecurityFilter(secEurCore, secUsdCore, secEurNonCore, secWatchlist).filter(client);
        var resolver = new LimitPriceResolver(wrapped, new TestCurrencyConverter(), AS_OF);

        // 1. All held securities (A=1000 EUR, B=2000 EUR, C=3000 EUR -> mean = 2,000.00 EUR across 3 positions)
        // < 50% AVG = 1,000.00 EUR -> for secEurCore (10 shares): < 100.00 EUR / share
        var halfAllAvg = new LimitPrice(RelationalOperator.SMALLER,
                        TargetMetric.AVG_POSITION_VALUE, 0.5d, null);
        var resAll = resolver.resolve(halfAllAvg, secEurCore);
        assertThat(resAll.status(), is(Status.RESOLVED));
        assertThat(resAll.groupCount(), is(3));
        assertThat(resAll.groupAverageValue(), is(Money.of(CurrencyUnit.EUR, amountOf(2000.00))));
        assertThat(resAll.resolvedQuote(), is(OptionalLong.of(quoteOf(100.00))));
        assertThat(halfAllAvg.isExceeded(new SecurityPrice(AS_OF, quoteOf(100.00)), resolver, secEurCore), is(false));
        assertThat(halfAllAvg.isExceeded(new SecurityPrice(AS_OF, quoteOf(99.99)), resolver, secEurCore), is(true));

        // 2. Checkbox filter matched by columnLabel ("core" case-insensitive)
        // Core held = A (1000 EUR) + B (2000 EUR) -> mean = 1,500.00 EUR across 2 positions
        // For secUsdCore (20 shares, USD): >= 200% * 1,500 EUR = 3,000 EUR = 3,476.40 USD -> / 20 shares = 173.82 USD
        var doubleCoreByCol = new LimitPrice(RelationalOperator.GREATER_OR_EQUAL,
                        TargetMetric.AVG_POSITION_VALUE, 2.0d, "core");
        var resUsdCore = resolver.resolve(doubleCoreByCol, secUsdCore);
        assertThat(resUsdCore.status(), is(Status.RESOLVED));
        assertThat(resUsdCore.groupCount(), is(2));
        assertThat(resUsdCore.matchedAttributeLabel(), is("Core"));
        assertThat(resUsdCore.groupAverageValue(), is(Money.of(CurrencyUnit.EUR, amountOf(1500.00))));
        assertThat(resUsdCore.resolvedQuote(), is(OptionalLong.of(quoteOf(173.82))));
        assertThat(doubleCoreByCol.isExceeded(new SecurityPrice(AS_OF, quoteOf(173.81)), resolver, secUsdCore),
                        is(false));
        assertThat(doubleCoreByCol.isExceeded(new SecurityPrice(AS_OF, quoteOf(173.82)), resolver, secUsdCore),
                        is(true));

        // 3. Checkbox filter matched by full attribute Name ("Core Holdings")
        var trimCoreByName = new LimitPrice(RelationalOperator.GREATER,
                        TargetMetric.AVG_POSITION_VALUE, 1.5d, "Core Holdings");
        // > 150% * 1,500 EUR = 2,250 EUR -> / 10 shares of secEurCore = 225.00 EUR
        assertThat(trimCoreByName.resolveValue(resolver, secEurCore), is(OptionalLong.of(quoteOf(225.00))));

        // 4. Zero-share / sold security returns NO_HOLDINGS and never triggers
        var resWatchlist = resolver.resolve(trimCoreByName, secWatchlist);
        assertThat(resWatchlist.status(), is(Status.NO_HOLDINGS));
        assertThat(resWatchlist.resolvedQuote().isEmpty(), is(true));
        assertThat(trimCoreByName.isExceeded(new SecurityPrice(AS_OF, quoteOf(10000.00)), resolver, secWatchlist),
                        is(false));

        var ppOnWatchlist = new LimitPrice(RelationalOperator.SMALLER,
                        TargetMetric.PURCHASE_PRICE_FIFO, 0.6d, null);
        assertThat(ppOnWatchlist.resolveValue(resolver, secWatchlist).isEmpty(), is(true));
        assertThat(ppOnWatchlist.isExceeded(new SecurityPrice(AS_OF, quoteOf(1.00)), resolver, secWatchlist),
                        is(false));

        // 5. Unknown checkbox group and empty checkbox group
        var unknownGroup = new LimitPrice(RelationalOperator.GREATER,
                        TargetMetric.AVG_POSITION_VALUE, 2.0d, "DoesNotExist");
        assertThat(resolver.resolve(unknownGroup, secEurCore).status(), is(Status.UNKNOWN_GROUP));
        assertThat(unknownGroup.resolveValue(resolver, secEurCore).isEmpty(), is(true));

        var emptyGroup = new LimitPrice(RelationalOperator.GREATER_OR_EQUAL,
                        TargetMetric.AVG_POSITION_VALUE, 1.5d, "Sat");
        assertThat(resolver.resolve(emptyGroup, secEurCore).status(), is(Status.EMPTY_GROUP));
        assertThat(emptyGroup.resolveValue(resolver, secEurCore).isEmpty(), is(true));

        // 6. Cache invalidation via client.markDirty() when checkbox attribute changes
        secEurNonCore.getAttributes().put(coreCheckbox, Boolean.TRUE);
        client.markDirty();
        // Now Core includes A (1000), B (2000), C (3000) -> mean = 2,000 EUR -> 150% / 10 shares = 300.00 EUR
        assertThat(trimCoreByName.resolveValue(resolver, secEurCore), is(OptionalLong.of(quoteOf(300.00))));
    }
}
