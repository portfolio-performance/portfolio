package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord.Trails;
import name.abuchen.portfolio.snapshot.trail.Trail;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;
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

    /**
     * Inbound and outbound deliveries contribute to the delta like buys and
     * sells, so they must appear in the trail as well.
     */
    @Test
    public void testDeliveries()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2021-01-31", Values.Quote.factorize(120)) //
                        .addTo(client);

        new PortfolioBuilder() //
                        .inbound_delivery(security, "2020-06-01", Values.Share.factorize(20),
                                        Values.Amount.factorize(2000)) //
                        .outbound_delivery(security, "2020-12-01", Values.Share.factorize(5),
                                        Values.Amount.factorize(560), 0, 0) //
                        .addTo(client);

        Interval interval = Interval.of(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-31"));

        assertTrailMatchesDelta(client, security, interval);

        // 15 shares at 120 at the end, plus the sale, minus the purchase;
        // guards against the delta and the trail agreeing on a wrong value
        LazySecurityPerformanceRecord record = LazySecurityPerformanceSnapshot
                        .create(client, new TestCurrencyConverter(), interval).getRecord(security)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(record.getDelta(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1800 + 560 - 2000))));
    }

    /**
     * A transfer between two portfolios moves the shares but neither adds to
     * nor takes from the delta, so the trail must not contain it (the
     * TRANSFER_OUT and TRANSFER_IN legs would otherwise skew the total).
     */
    @Test
    public void testTransferBetweenPortfolios()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2021-01-31", Values.Quote.factorize(120)) //
                        .addTo(client);

        Portfolio portfolioA = new PortfolioBuilder() //
                        .buy(security, "2020-06-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        Portfolio portfolioB = new PortfolioBuilder().addTo(client);

        PortfolioTransferEntry transfer = new PortfolioTransferEntry(portfolioA, portfolioB);
        transfer.setSecurity(security);
        transfer.setDate(LocalDateTime.parse("2020-09-01T00:00"));
        transfer.setShares(Values.Share.factorize(10));
        transfer.setAmount(Values.Amount.factorize(1100));
        transfer.setCurrencyCode(security.getCurrencyCode());
        transfer.insert();

        Interval interval = Interval.of(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-31"));

        assertTrailMatchesDelta(client, security, interval);

        // only the purchase and the valuation at the end count; guards against
        // the delta and the trail agreeing on a wrong value
        LazySecurityPerformanceRecord record = LazySecurityPerformanceSnapshot
                        .create(client, new TestCurrencyConverter(), interval).getRecord(security)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(record.getDelta(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1200 - 1000))));
    }

    /**
     * Whether the trail contains an exchange rate step that converts a
     * transaction of the given label.
     */
    private boolean containsConversionOf(TrailRecord trail, String transactionLabel)
    {
        if (trail.getLabel().startsWith(Messages.CSVColumn_ExchangeRate) && trail.getInputs().size() == 1
                        && transactionLabel.equals(trail.getInputs().get(0).getLabel()))
            return true;

        return trail.getInputs().stream().anyMatch(input -> containsConversionOf(input, transactionLabel));
    }

    /**
     * A transaction in a currency other than the term currency must be
     * converted in the trail, just like the valuation at the end. The
     * {@link PortfolioBuilder} books all transactions in EUR, so the USD
     * delivery is created directly.
     */
    @Test
    public void testForeignCurrencyTransaction()
    {
        Client client = new Client();

        Security security = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2021-01-31", Values.Quote.factorize(130)) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder().addTo(client);
        portfolio.addTransaction(new PortfolioTransaction(LocalDate.parse("2020-06-01").atStartOfDay(),
                        CurrencyUnit.USD, Values.Amount.factorize(1000), security, Values.Share.factorize(10),
                        PortfolioTransaction.Type.DELIVERY_INBOUND, 0, 0));

        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(),
                        Interval.of(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-31")));
        LazySecurityPerformanceRecord record = snapshot.getRecord(security)
                        .orElseThrow(IllegalArgumentException::new);

        Trail trail = record.explain(Trails.ABSOLUTE_PERFORMANCE).orElseThrow(IllegalArgumentException::new);

        assertThat(trail.getRecord().getValue(), is(record.getDelta()));
        assertThat(containsConversionOf(trail.getRecord(), PortfolioTransaction.Type.DELIVERY_INBOUND.toString()),
                        is(true));
    }

    /**
     * Transfers do not contribute to the delta, so the calculation must not
     * read their amount: a transfer created without a currency code (which
     * the public API allows) must not fail the calculation.
     */
    @Test
    public void testTransferWithoutCurrencyCode()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2020-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2021-01-31", Values.Quote.factorize(120)) //
                        .addTo(client);

        Portfolio portfolioA = new PortfolioBuilder() //
                        .buy(security, "2020-06-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        Portfolio portfolioB = new PortfolioBuilder().addTo(client);

        PortfolioTransferEntry transfer = new PortfolioTransferEntry(portfolioA, portfolioB);
        transfer.setSecurity(security);
        transfer.setDate(LocalDateTime.parse("2020-09-01T00:00"));
        transfer.setShares(Values.Share.factorize(10));
        transfer.insert();

        Interval interval = Interval.of(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-31"));

        assertTrailMatchesDelta(client, security, interval);
    }
}
