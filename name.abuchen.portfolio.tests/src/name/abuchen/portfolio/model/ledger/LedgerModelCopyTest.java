package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.FeeReason;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests low-level ledger model behavior used by higher-level transaction flows.
 * These tests make sure copying, graph links, and core model fields keep ledger data consistent.
 */
@SuppressWarnings("nls")
public class LedgerModelCopyTest
{
    /**
     * Checks the Ledger-V6 scenario: copy entry preserves complete entry graph.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testCopyEntryPreservesCompleteEntryGraph()
    {
        var original = entryGraph();
        var copy = LedgerModelCopy.copyEntry(original);

        assertNotSame(original, copy);
        assertThat(copy.getUUID(), is(original.getUUID()));
        assertThat(copy.getType(), is(original.getType()));
        assertThat(copy.getDateTime(), is(original.getDateTime()));
        assertThat(copy.getNote(), is(original.getNote()));
        assertThat(copy.getSource(), is(original.getSource()));
        assertThat(copy.getUpdatedAt(), is(original.getUpdatedAt()));

        assertThat(copy.getParameters().size(), is(2));
        assertParameterCopied(original.getParameters().get(0), copy.getParameters().get(0));
        assertParameterCopied(original.getParameters().get(1), copy.getParameters().get(1));

        assertThat(copy.getPostings().size(), is(2));
        assertPostingCopied(original.getPostings().get(0), copy.getPostings().get(0));
        assertPostingCopied(original.getPostings().get(1), copy.getPostings().get(1));

        assertThat(copy.getProjectionRefs().size(), is(2));
        assertProjectionCopied(original.getProjectionRefs().get(0), copy.getProjectionRefs().get(0));
        assertProjectionCopied(original.getProjectionRefs().get(1), copy.getProjectionRefs().get(1));
    }

    /**
     * Checks the Ledger-V6 scenario: copy ledger preserves entry order and separates collections.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testCopyLedgerPreservesEntryOrderAndSeparatesCollections()
    {
        var first = entryGraph();
        var second = new LedgerEntry("entry-2");
        var ledger = new Ledger();

        second.setType(LedgerEntryType.DEPOSIT);
        second.setDateTime(LocalDateTime.of(2026, 1, 4, 0, 0));
        ledger.addEntry(first);
        ledger.addEntry(second);

        var copy = LedgerModelCopy.copyLedger(ledger);

        assertNotSame(ledger, copy);
        assertThat(copy.getEntries().stream().map(LedgerEntry::getUUID).toList(), is(
                        ledger.getEntries().stream().map(LedgerEntry::getUUID).toList()));
        assertNotSame(ledger.getEntries().get(0), copy.getEntries().get(0));
        assertNotSame(ledger.getEntries().get(1), copy.getEntries().get(1));

        copy.getEntries().get(0).setNote("changed copy");

        assertThat(first.getNote(), is("source note"));
        assertThat(copy.getEntries().get(0).getNote(), is("changed copy"));
    }

    /**
     * Checks the Ledger-V6 scenario: copy entry does not share mutable child objects.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testCopyEntryDoesNotShareMutableChildObjects()
    {
        var original = entryGraph();
        var copy = LedgerModelCopy.copyEntry(original);

        copy.removeParameter(copy.getParameters().get(0));
        copy.getPostings().get(1).setShares(Values.Share.factorize(99));
        copy.getPostings().get(0).removeParameter(copy.getPostings().get(0).getParameters().get(0));
        copy.getProjectionRefs().get(0).setPrimaryPostingUUID("changed-posting");
        copy.getProjectionRefs().get(0).getMemberships().get(0).setPostingUUID("changed-membership");

        assertThat(original.getParameters().size(), is(2));
        assertThat(original.getPostings().get(1).getShares(), is(Values.Share.factorize(10)));
        assertThat(original.getPostings().get(0).getParameters().size(), is(2));
        assertThat(original.getProjectionRefs().get(0).getPrimaryPostingUUID(), is("posting-1"));
        assertThat(original.getProjectionRefs().get(0).getMemberships().get(0).getPostingUUID(), is("posting-1"));
    }

    private LedgerEntry entryGraph()
    {
        var account = new Account();
        var portfolio = new Portfolio();
        var security = new Security("Siemens", CurrencyUnit.EUR);
        var entry = new LedgerEntry("entry-1");
        var cash = new LedgerPosting("posting-1");
        var securityPosting = new LedgerPosting("posting-2");
        var accountProjection = new LedgerProjectionRef("projection-1");
        var portfolioProjection = new LedgerProjectionRef("projection-2");

        entry.setType(LedgerEntryType.BUY);
        entry.setDateTime(LocalDateTime.of(2026, 1, 2, 0, 0));
        entry.setNote("source note");
        entry.setSource("source system");
        entry.addParameter(LedgerParameter.ofString(LedgerParameterType.EVENT_REFERENCE, "event-1"));
        entry.addParameter(LedgerParameter.ofBoolean(LedgerParameterType.MANUAL_VALUATION_OVERRIDE, true));

        cash.setType(LedgerPostingType.CASH);
        cash.setAmount(Values.Amount.factorize(100));
        cash.setCurrency(CurrencyUnit.EUR);
        cash.setForexAmount(Values.Amount.factorize(110));
        cash.setForexCurrency(CurrencyUnit.USD);
        cash.setExchangeRate(new BigDecimal("0.9091"));
        cash.setAccount(account);
        cash.addParameter(LedgerParameter.ofString(LedgerParameterType.FEE_REASON, FeeReason.BROKER_FEE.getCode()));
        cash.addParameter(LedgerParameter.ofMoney(LedgerParameterType.FAIR_MARKET_VALUE,
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100))));

        securityPosting.setType(LedgerPostingType.SECURITY);
        securityPosting.setAmount(Values.Amount.factorize(100));
        securityPosting.setCurrency(CurrencyUnit.EUR);
        securityPosting.setSecurity(security);
        securityPosting.setShares(Values.Share.factorize(10));
        securityPosting.setPortfolio(portfolio);

        accountProjection.setRole(LedgerProjectionRole.ACCOUNT);
        accountProjection.setAccount(account);
        accountProjection.setPrimaryPosting(cash);
        accountProjection.setPostingGroup(cash);
        accountProjection.addMembership(cash.getUUID(), ProjectionMembershipRole.PRIMARY);
        accountProjection.addMembership(cash.getUUID(), ProjectionMembershipRole.GROUP_ANCHOR);

        portfolioProjection.setRole(LedgerProjectionRole.PORTFOLIO);
        portfolioProjection.setPortfolio(portfolio);
        portfolioProjection.setPrimaryPosting(securityPosting);
        portfolioProjection.addMembership(securityPosting.getUUID(), ProjectionMembershipRole.PRIMARY);

        entry.addPosting(cash);
        entry.addPosting(securityPosting);
        entry.addProjectionRef(accountProjection);
        entry.addProjectionRef(portfolioProjection);
        entry.setUpdatedAt(Instant.parse("2026-06-20T12:00:00Z"));

        return entry;
    }

    private void assertPostingCopied(LedgerPosting original, LedgerPosting copy)
    {
        assertNotSame(original, copy);
        assertThat(copy.getUUID(), is(original.getUUID()));
        assertThat(copy.getType(), is(original.getType()));
        assertThat(copy.getAmount(), is(original.getAmount()));
        assertThat(copy.getCurrency(), is(original.getCurrency()));
        assertThat(copy.getForexAmount(), is(original.getForexAmount()));
        assertThat(copy.getForexCurrency(), is(original.getForexCurrency()));
        assertThat(copy.getExchangeRate(), is(original.getExchangeRate()));
        assertSame(original.getSecurity(), copy.getSecurity());
        assertThat(copy.getShares(), is(original.getShares()));
        assertSame(original.getAccount(), copy.getAccount());
        assertSame(original.getPortfolio(), copy.getPortfolio());
        assertThat(copy.getParameters().size(), is(original.getParameters().size()));

        for (var index = 0; index < original.getParameters().size(); index++)
            assertParameterCopied(original.getParameters().get(index), copy.getParameters().get(index));
    }

    private void assertProjectionCopied(LedgerProjectionRef original, LedgerProjectionRef copy)
    {
        assertNotSame(original, copy);
        assertThat(copy.getUUID(), is(original.getUUID()));
        assertThat(copy.getRole(), is(original.getRole()));
        assertSame(original.getAccount(), copy.getAccount());
        assertSame(original.getPortfolio(), copy.getPortfolio());
        assertThat(copy.getPrimaryPostingUUID(), is(original.getPrimaryPostingUUID()));
        assertThat(copy.getPostingGroupUUID(), is(original.getPostingGroupUUID()));
        assertThat(copy.getMemberships().size(), is(original.getMemberships().size()));

        for (var index = 0; index < original.getMemberships().size(); index++)
            assertProjectionMembershipCopied(original.getMemberships().get(index), copy.getMemberships().get(index));
    }

    private void assertProjectionMembershipCopied(ProjectionMembership original, ProjectionMembership copy)
    {
        assertNotSame(original, copy);
        assertThat(copy.getPostingUUID(), is(original.getPostingUUID()));
        assertThat(copy.getRole(), is(original.getRole()));
    }

    private void assertParameterCopied(LedgerParameter<?> original, LedgerParameter<?> copy)
    {
        assertNotSame(original, copy);
        assertThat(copy.getType(), is(original.getType()));
        assertThat(copy.getValueKind(), is(original.getValueKind()));
        assertSame(original.getValue(), copy.getValue());
    }
}
