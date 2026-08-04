package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord.Trails;
import name.abuchen.portfolio.snapshot.trail.Trail;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class DeltaCalculationTest
{
    /**
     * The absolute performance trail must reconstruct exactly the value shown
     * in the "Absolute performance" column, i.e.
     * {@link LazySecurityPerformanceRecord#getDelta()}.
     */
    private void assertTrailMatchesDelta(Client client, Security security, Interval interval)
    {
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);
        LazySecurityPerformanceRecord record = snapshot.getRecord(security)
                        .orElseThrow(IllegalArgumentException::new);

        Trail trail = record.explain(Trails.ABSOLUTE_PERFORMANCE).orElseThrow(IllegalArgumentException::new);

        assertThat(trail.getRecord().getValue(), is(record.getDelta()));
    }

    /**
     * Adds a security-linked account transaction (the {@link AccountBuilder}
     * fee/tax helpers do not attach a security, but only security-linked
     * transactions reach {@link DeltaCalculation}).
     */
    private void addSecurityTransaction(Account account, AccountTransaction.Type type, String date, long amount,
                    Security security)
    {
        account.addTransaction(new AccountTransaction(LocalDate.parse(date).atStartOfDay(), CurrencyUnit.EUR, amount,
                        security, type));
    }

    @Test
    public void testBuyAndEndValuation()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2021-01-31", Values.Quote.factorize(120)) //
                        .addTo(client);

        new PortfolioBuilder() //
                        .buy(security, "2020-06-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        assertTrailMatchesDelta(client, security,
                        Interval.of(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-31")));
    }

    @Test
    public void testBuySellAndDividend()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2021-01-31", Values.Quote.factorize(120)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .dividend("2020-09-01", Values.Amount.factorize(25), security) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2020-06-01", Values.Share.factorize(20), Values.Amount.factorize(2000)) //
                        .sell(security, "2020-12-01", Values.Share.factorize(5), Values.Amount.factorize(560)) //
                        .addTo(client);

        assertTrailMatchesDelta(client, security,
                        Interval.of(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-31")));
    }

    /**
     * A security whose only line item in the interval is a standalone fee (no
     * holdings, no valuation at start/end, no trades). The delta is a pure
     * outflow, so the trail must still reconstruct it rather than being empty.
     */
    @Test
    public void testOnlyOutflow()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addTo(client);

        Account account = new AccountBuilder().addTo(client);
        account.addTransaction(new AccountTransaction(LocalDate.parse("2020-07-01").atStartOfDay(),
                        CurrencyUnit.EUR, Values.Amount.factorize(10), security, AccountTransaction.Type.FEES));

        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(),
                        Interval.of(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-31")));
        LazySecurityPerformanceRecord record = snapshot.getRecord(security)
                        .orElseThrow(IllegalArgumentException::new);

        // sanity: the delta really is a non-zero outflow
        assertThat(record.getDelta(), is(Money.of(CurrencyUnit.EUR, -Values.Amount.factorize(10))));

        Trail trail = record.explain(Trails.ABSOLUTE_PERFORMANCE).orElseThrow(IllegalArgumentException::new);
        assertThat(trail.getRecord().getValue(), is(record.getDelta()));
    }

    /**
     * Locks the trail-total-equals-delta invariant across <em>every</em>
     * contributing line-item type at once: valuation at start and end, buy,
     * sell, dividend, taxes, tax refund, fees and fee refund. If a future
     * change updates {@code delta} for one of these without adding the matching
     * entry to the positive/negative trail lists (or vice versa), the trail
     * total will diverge from {@link LazySecurityPerformanceRecord#getDelta()}
     * and this test will fail.
     */
    @Test
    public void testAllContributionTypes()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2019-12-31", Values.Quote.factorize(100)) //
                        .addPrice("2021-01-31", Values.Quote.factorize(120)) //
                        .addTo(client);

        // a position held before the interval start produces a valuation at
        // start; shares still held at the end produce a valuation at end
        Account account = new AccountBuilder() //
                        .dividend("2020-09-01", Values.Amount.factorize(25), security) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2019-06-01", Values.Share.factorize(20), Values.Amount.factorize(2000)) //
                        .buy(security, "2020-06-01", Values.Share.factorize(10), Values.Amount.factorize(1100)) //
                        .sell(security, "2020-12-01", Values.Share.factorize(5), Values.Amount.factorize(560)) //
                        .addTo(client);

        addSecurityTransaction(account, AccountTransaction.Type.TAXES, "2020-09-02", Values.Amount.factorize(7),
                        security);
        addSecurityTransaction(account, AccountTransaction.Type.TAX_REFUND, "2020-10-01", Values.Amount.factorize(3),
                        security);
        addSecurityTransaction(account, AccountTransaction.Type.FEES, "2020-07-01", Values.Amount.factorize(10),
                        security);
        addSecurityTransaction(account, AccountTransaction.Type.FEES_REFUND, "2020-08-01", Values.Amount.factorize(4),
                        security);

        assertTrailMatchesDelta(client, security,
                        Interval.of(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-31")));
    }

    @Test
    public void testForeignCurrencySecurity()
    {
        Client client = new Client();

        Security security = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2021-01-31", Values.Quote.factorize(130)) //
                        .addTo(client);

        new PortfolioBuilder() //
                        .buy(security, "2020-06-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        assertTrailMatchesDelta(client, security,
                        Interval.of(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-31")));
    }
}
