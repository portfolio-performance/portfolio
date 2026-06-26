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
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

/**
 * Tests low-level ledger model behavior used by higher-level transaction flows.
 * These tests make sure copying, graph links, and core model fields keep ledger data consistent.
 */
@SuppressWarnings("nls")
public class LedgerGraphWriterTest
{
    /**
     * Checks the Ledger-V6 scenario: add remove and replace entry.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testAddRemoveAndReplaceEntry()
    {
        var ledger = new Ledger();
        var current = entry("entry-1");
        var replacement = entry("entry-2");

        LedgerGraphWriter.addEntry(ledger, current);

        assertThat(ledger.getEntries().size(), is(1));
        assertSame(current, ledger.getEntries().get(0));

        LedgerGraphWriter.replaceEntry(ledger, current, replacement);

        assertThat(ledger.getEntries().size(), is(1));
        assertSame(replacement, ledger.getEntries().get(0));

        LedgerGraphWriter.removeEntry(ledger, replacement);

        assertThat(ledger.getEntries().size(), is(0));
    }

    /**
     * Checks the Ledger-V6 scenario: replace entry contents preserves source graph and separates mutable children.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testReplaceEntryContentsPreservesSourceGraphAndSeparatesMutableChildren()
    {
        var source = entry("source-entry");
        var target = entry("target-entry");

        LedgerGraphWriter.replaceEntryContents(target, source);

        assertThat(target.getUUID(), is(source.getUUID()));
        assertThat(target.getType(), is(source.getType()));
        assertThat(target.getDateTime(), is(source.getDateTime()));
        assertThat(target.getNote(), is(source.getNote()));
        assertThat(target.getSource(), is(source.getSource()));
        assertThat(target.getUpdatedAt(), is(source.getUpdatedAt()));

        assertThat(target.getParameters().size(), is(2));
        assertThat(target.getParameters().get(0).getType(), is(LedgerParameterType.EVENT_REFERENCE));
        assertThat(target.getParameters().get(1).getType(), is(LedgerParameterType.RATIO_NUMERATOR));

        assertThat(target.getPostings().size(), is(2));
        assertThat(target.getPostings().get(0).getUUID(), is("source-entry-cash"));
        assertThat(target.getPostings().get(1).getUUID(), is("source-entry-security"));

        assertThat(target.getProjectionRefs().size(), is(2));
        assertThat(target.getProjectionRefs().get(0).getUUID(), is("source-entry-account"));
        assertThat(target.getProjectionRefs().get(0).getPrimaryPostingUUID(), is("source-entry-cash"));
        assertThat(target.getProjectionRefs().get(0).getPostingGroupUUID(), is("source-entry-security"));
        assertThat(target.getProjectionRefs().get(1).getUUID(), is("source-entry-portfolio"));
        assertThat(target.getProjectionRefs().get(1).getPrimaryPostingUUID(), is("source-entry-security"));

        assertNotSame(source.getPostings().get(0), target.getPostings().get(0));
        assertNotSame(source.getProjectionRefs().get(0), target.getProjectionRefs().get(0));

        target.getPostings().get(0).setAmount(Values.Amount.factorize(200));
        target.getProjectionRefs().get(0).setPrimaryPostingUUID("changed");

        assertThat(source.getPostings().get(0).getAmount(), is(Values.Amount.factorize(100)));
        assertThat(source.getProjectionRefs().get(0).getPrimaryPostingUUID(), is("source-entry-cash"));
    }

    private LedgerEntry entry(String uuid)
    {
        var account = new Account();
        var portfolio = new Portfolio();
        var security = new Security("Security", CurrencyUnit.EUR);
        var entry = new LedgerEntry(uuid);
        var cash = new LedgerPosting(uuid + "-cash");
        var securityPosting = new LedgerPosting(uuid + "-security");
        var accountProjection = new LedgerProjectionRef(uuid + "-account");
        var portfolioProjection = new LedgerProjectionRef(uuid + "-portfolio");

        entry.setType(LedgerEntryType.BUY);
        entry.setDateTime(LocalDateTime.of(2026, 1, 2, 0, 0));
        entry.setNote("note-" + uuid);
        entry.setSource("source-" + uuid);
        entry.addParameter(LedgerParameter.ofString(LedgerParameterType.EVENT_REFERENCE, uuid + "-event"));
        entry.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_NUMERATOR, BigDecimal.ONE));

        cash.setType(LedgerPostingType.CASH);
        cash.setAmount(Values.Amount.factorize(100));
        cash.setCurrency(CurrencyUnit.EUR);
        cash.setAccount(account);

        securityPosting.setType(LedgerPostingType.SECURITY);
        securityPosting.setSecurity(security);
        securityPosting.setShares(Values.Share.factorize(10));
        securityPosting.setPortfolio(portfolio);

        accountProjection.setRole(LedgerProjectionRole.ACCOUNT);
        accountProjection.setAccount(account);
        accountProjection.setPrimaryPosting(cash);
        accountProjection.setPostingGroup(securityPosting);

        portfolioProjection.setRole(LedgerProjectionRole.PORTFOLIO);
        portfolioProjection.setPortfolio(portfolio);
        portfolioProjection.setPrimaryPosting(securityPosting);

        entry.addPosting(cash);
        entry.addPosting(securityPosting);
        entry.addProjectionRef(accountProjection);
        entry.addProjectionRef(portfolioProjection);
        entry.setUpdatedAt(Instant.parse("2026-06-20T12:00:00Z"));

        return entry;
    }
}
