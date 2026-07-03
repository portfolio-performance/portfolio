package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.Test;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.CorporateActionEntry.LegRole;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class CorporateActionEntryTest
{
    @Test
    public void testPrimaryCounterpartPairsSourceAndTarget()
    {
        Portfolio portfolio = new Portfolio();
        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        var target = new PortfolioTransaction();
        target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);

        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.addLeg(portfolio, source, LegRole.SOURCE);
        entry.addLeg(portfolio, target, LegRole.TARGET);

        assertThat(entry.getCrossTransaction(source), is(target));
        assertThat(entry.getCrossTransaction(target), is(source));
        assertThat(entry.getCrossOwner(source), is(portfolio));
        assertThat(entry.getLegs().size(), is(2));
        assertThat(entry.getLeg(LegRole.TARGET).orElseThrow(), is(target));
    }

    @Test
    public void testPropertyBagRoundTrips()
    {
        var entry = new CorporateActionEntry();
        entry.setExDate(java.time.LocalDate.parse("2010-06-01"));
        entry.setBasisRatio(new java.math.BigDecimal("0.25"));
        entry.setDistributionRatio(new CorporateActionEntry.DistributionRatio(1, 3));
        entry.setReferencePrice(name.abuchen.portfolio.money.Values.Quote.factorize(20));

        assertThat(entry.getExDate(), is(java.time.LocalDate.parse("2010-06-01")));
        assertThat(entry.getBasisRatio(), is(new java.math.BigDecimal("0.25")));
        assertThat(entry.getDistributionRatio().numerator(), is(1));
        assertThat(entry.getDistributionRatio().denominator(), is(3));
        assertThat(entry.getReferencePrice(), is(name.abuchen.portfolio.money.Values.Quote.factorize(20)));
    }

    /**
     * Builds a four-leg SPIN_OFF in one portfolio + one account: the two
     * mandatory distribution legs (SOURCE/TARGET on the portfolio) plus a
     * fractional disposal (FRACTION_SALE on the portfolio, CASH_IN_LIEU on the
     * account). All four legs are wired to the entry and inserted into their
     * owners; the entry is registered on the client. Mirrors what the Phase 6
     * dialog will produce, but built directly so Phase 5 can test the group
     * semantics without any UI.
     */
    private Client buildFourLegSpinOff()
    {
        Client client = new Client();

        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        client.addPortfolio(portfolio);

        Account account = new Account("account");
        client.addAccount(account);

        var exDate = java.time.LocalDateTime.parse("2015-01-09T00:00");

        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setDateTime(exDate);
        source.setSecurity(parent);
        source.setCurrencyCode(CurrencyUnit.EUR);

        var target = new PortfolioTransaction();
        target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        target.setDateTime(exDate);
        target.setSecurity(spinco);
        target.setCurrencyCode(CurrencyUnit.EUR);

        var fractionSale = new PortfolioTransaction();
        fractionSale.setType(PortfolioTransaction.Type.SELL);
        fractionSale.setDateTime(exDate);
        fractionSale.setSecurity(spinco);
        fractionSale.setCurrencyCode(CurrencyUnit.EUR);

        var cashInLieu = new AccountTransaction();
        cashInLieu.setType(AccountTransaction.Type.SELL);
        cashInLieu.setDateTime(exDate);
        cashInLieu.setSecurity(spinco);
        cashInLieu.setCurrencyCode(CurrencyUnit.EUR);

        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.addLeg(portfolio, source, LegRole.SOURCE);
        entry.addLeg(portfolio, target, LegRole.TARGET);
        entry.addLeg(portfolio, fractionSale, LegRole.FRACTION_SALE);
        entry.addLeg(account, cashInLieu, LegRole.CASH_IN_LIEU);
        entry.insert();

        client.addCorporateAction(entry);

        return client;
    }

    @Test
    public void testDeletingPrimaryLegRemovesAllLegsAndTheRegistryEntry()
    {
        Client client = buildFourLegSpinOff();
        Portfolio portfolio = client.getPortfolios().get(0);
        Account account = client.getAccounts().get(0);

        assertThat(portfolio.getTransactions(), hasSize(3));
        assertThat(account.getTransactions(), hasSize(1));
        assertThat(client.getCorporateActions(), hasSize(1));

        // delete via the SOURCE leg (the primary counterpart path)
        var source = portfolio.getTransactions().stream()
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND).findFirst()
                        .orElseThrow();
        portfolio.deleteTransaction(source, client);

        // every leg is gone from both owners (binary cascade would orphan the
        // fraction legs) ...
        assertThat(portfolio.getTransactions(), hasSize(0));
        assertThat(account.getTransactions(), hasSize(0));
        // ... and the registry entry is gone (so it can't be persisted dangling)
        assertThat(client.getCorporateActions(), hasSize(0));
    }

    @Test
    public void testDeletingAccountLegAlsoRemovesTheWholeGroup()
    {
        Client client = buildFourLegSpinOff();
        Portfolio portfolio = client.getPortfolios().get(0);
        Account account = client.getAccounts().get(0);

        // delete via the CASH_IN_LIEU account leg (a non-primary leg on a
        // different owner) — deletion must be entry-point independent
        var cashInLieu = account.getTransactions().get(0);
        account.deleteTransaction(cashInLieu, client);

        assertThat(portfolio.getTransactions(), hasSize(0));
        assertThat(account.getTransactions(), hasSize(0));
        assertThat(client.getCorporateActions(), hasSize(0));
    }

    @Test
    public void testBinaryCrossEntryStillDeletesOnlyItsTwoTransactions()
    {
        // a two-leg SPIN_OFF still deletes exactly its two legs (the N-ary path
        // reduces to the binary case for a 2-leg entry)
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        client.addPortfolio(portfolio);

        var unrelated = new PortfolioTransaction();
        unrelated.setType(PortfolioTransaction.Type.BUY);
        unrelated.setDateTime(java.time.LocalDateTime.parse("2014-01-01T00:00"));
        unrelated.setSecurity(parent);
        unrelated.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.addTransaction(unrelated);

        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setSecurity(parent);
        source.setCurrencyCode(CurrencyUnit.EUR);
        var target = new PortfolioTransaction();
        target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        target.setSecurity(spinco);
        target.setCurrencyCode(CurrencyUnit.EUR);

        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.addLeg(portfolio, source, LegRole.SOURCE);
        entry.addLeg(portfolio, target, LegRole.TARGET);
        entry.insert();
        client.addCorporateAction(entry);

        portfolio.deleteTransaction(target, client);

        // the unrelated BUY survives; both distribution legs and the entry go
        assertThat(portfolio.getTransactions(), contains(unrelated));
        assertThat(client.getCorporateActions(), hasSize(0));
    }

    @Test
    public void testUpdateFromCascadesDateAndNoteButNotSecurityOrShares()
    {
        Client client = buildFourLegSpinOff();
        Portfolio portfolio = client.getPortfolios().get(0);

        var source = portfolio.getTransactions().stream()
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND).findFirst()
                        .orElseThrow();
        var target = portfolio.getTransactions().stream()
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DISTRIBUTION_INBOUND).findFirst()
                        .orElseThrow();

        // the target keeps its own security (spinco) and its own shares (a fact)
        Security spinco = target.getSecurity();
        target.setShares(name.abuchen.portfolio.money.Values.Share.factorize(100));
        long targetSharesBefore = target.getShares();

        // edit the SOURCE leg's shared attributes, then cascade
        var newDate = java.time.LocalDateTime.parse("2015-02-01T00:00");
        source.setDateTime(newDate);
        source.setNote("spin-off adjusted");

        client.getCorporateActions().get(0).updateFrom(source);

        // date + note cascaded to the sibling ...
        assertThat(target.getDateTime(), is(newDate));
        assertThat(target.getNote(), is("spin-off adjusted"));
        // ... but the per-leg security and shares are untouched (spec §11)
        assertThat(target.getSecurity(), is(spinco));
        assertThat(target.getShares(), is(targetSharesBefore));
        assertThat(source.getSecurity(), is(not(spinco))); // source is still the parent
    }

    @Test
    public void testSetOwnerReassignsOnlyTheAddressedLeg()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolioA = new Portfolio();
        portfolioA.setName("A");
        Portfolio portfolioB = new Portfolio();
        portfolioB.setName("B");

        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setSecurity(parent);
        var target = new PortfolioTransaction();
        target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        target.setSecurity(spinco);

        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.addLeg(portfolioA, source, LegRole.SOURCE);
        entry.addLeg(portfolioA, target, LegRole.TARGET);

        // reassign only the SOURCE leg to portfolio B
        entry.setOwner(source, portfolioB);

        assertThat(entry.getOwner(source), is(portfolioB));
        assertThat(entry.getOwner(target), is(portfolioA)); // sibling unaffected
        // target's cross owner is the SOURCE leg's owner, now B
        assertThat(entry.getCrossOwner(target), is(portfolioB));
    }

    @Test
    public void testDeleteRemovesEntryFromRegistryButInsertDoesNotRestoreIt()
    {
        // Characterization of a deliberate delete/insert asymmetry:
        // deleteTransaction removes the CorporateActionEntry from the client
        // registry, but CorporateActionEntry.insert() only re-adds the legs to
        // their owners -- it does NOT re-register the entry (insert() is shared
        // with paths that must not touch the registry). The inline
        // owner-reassignment idiom (delete -> setOwner -> insert) would
        // therefore have dropped the entry; Phase 6 closes that by refusing
        // inline owner edits on CA legs (TransactionOwnerListEditingSupport.
        // canEdit returns false), so owner changes go through the dialog. This
        // test locks the asymmetry so a future change to insert()/the registry
        // wiring is caught.
        Client client = buildFourLegSpinOff();
        Portfolio portfolio = client.getPortfolios().get(0);
        Account account = client.getAccounts().get(0);
        CorporateActionEntry entry = client.getCorporateActions().get(0);

        var source = portfolio.getTransactions().stream()
                        .filter(t -> t.getType() == PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND).findFirst()
                        .orElseThrow();
        portfolio.deleteTransaction(source, client);

        assertThat(client.getCorporateActions(), hasSize(0));

        // re-inserting restores the legs to their owners ...
        entry.insert();
        assertThat(portfolio.getTransactions(), hasSize(3));
        assertThat(account.getTransactions(), hasSize(1));
        // ... but the entry is NOT back in the registry (the documented gap)
        assertThat(client.getCorporateActions(), hasSize(0));
    }
}
