package name.abuchen.portfolio.snapshot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.CorporateActionEntry.LegRole;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;

/**
 * Exercises {@code ClientSnapshotIterator#nextValuation(LocalDate)} (package
 * private) through its only production entry point,
 * {@link PerformanceIndex#forClient}, which is driven by {@link ClientIndex}.
 */
@SuppressWarnings("nls")
public class ClientSnapshotIteratorTest
{
    @Test
    public void testDistributionOutboundDoesNotZeroParentValuation()
    {
        Client client = new Client();

        // no price is seeded at all: Security#getSecurityPrice returns a
        // 0-valued SecurityPrice for the requested date, forcing
        // nextValuation into the last-transaction-price fallback
        Security parent = new SecurityBuilder().addTo(client);

        // fund the reference account first so the buy leaves it at exactly
        // 0; otherwise the account's negative balance from an unfunded buy
        // would offset the (buggy) zeroed position and mask the bug
        Account account = new AccountBuilder() //
                        .deposit_("2010-01-01", Values.Amount.factorize(5000)) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder(account) //
                        .buy(parent, "2010-01-01", Values.Share.factorize(100), Values.Amount.factorize(5000)) //
                        .addTo(client);

        // latest transaction on the parent is a shares-0 spin-off source leg
        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setDateTime(LocalDateTime.parse("2010-06-01T00:00"));
        source.setSecurity(parent);
        source.setShares(0);
        source.setCurrencyCode(CurrencyUnit.EUR);
        source.setAmount(Values.Amount.factorize(2000));

        CorporateActionEntry entry = new CorporateActionEntry();
        entry.setBasisRatio(new java.math.BigDecimal("0.25"));
        entry.addLeg(portfolio, source, LegRole.SOURCE);
        portfolio.addTransaction(source);

        Interval period = Interval.of(LocalDate.parse("2009-12-31"), LocalDate.parse("2010-12-31"));
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, period, new ArrayList<>());

        int endIndex = Arrays.binarySearch(index.getDates(), LocalDate.parse("2010-12-31"));

        // fallback must use the last transaction that actually carries
        // shares (the buy: 5,000.00 / 100 shares = 50.00/share), not the
        // shares-0 outbound leg: 100 shares * 50.00 = 5,000.00, NOT 0
        assertThat(index.getTotals()[endIndex], is(Values.Amount.factorize(5000)));
    }
}
