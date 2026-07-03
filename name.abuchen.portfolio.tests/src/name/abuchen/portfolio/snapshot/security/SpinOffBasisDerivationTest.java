package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.CorporateActionEntry.LegRole;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;

/**
 * Phase 0 spike: proves the cross-security cost-basis derivation (spec §7)
 * works through the real per-security snapshot record path.
 */
@SuppressWarnings("nls")
public class SpinOffBasisDerivationTest
{
    @Test
    public void testTargetBasisDerivedFromSourceAtExDate()
    {
        Client client = new Client();

        Security parent = new SecurityBuilder().addTo(client);
        Security spinco = new SecurityBuilder().addTo(client);

        // 100 parent shares, cost basis 5,000.00
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(parent, "2010-01-01", Values.Share.factorize(100), Values.Amount.factorize(5000)) //
                        .addTo(client);

        var exDate = LocalDateTime.parse("2010-06-01T00:00");

        // source leg: 25% of parent basis leaves, shares unchanged
        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setDateTime(exDate);
        source.setSecurity(parent);
        source.setShares(0);
        source.setCurrencyCode(CurrencyUnit.EUR);
        source.setAmount(Values.Amount.factorize(2000));

        // target leg: 100 spinco shares received, basis derived from parent
        var target = new PortfolioTransaction();
        target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        target.setDateTime(exDate);
        target.setSecurity(spinco);
        target.setShares(Values.Share.factorize(100));
        target.setCurrencyCode(CurrencyUnit.EUR);
        target.setAmount(Values.Amount.factorize(2000));

        CorporateActionEntry entry = new CorporateActionEntry();
        entry.setBasisRatio(new BigDecimal("0.25"));
        entry.addLeg(portfolio, source, LegRole.SOURCE);
        entry.addLeg(portfolio, target, LegRole.TARGET);

        portfolio.addTransaction(source);
        portfolio.addTransaction(target);

        CurrencyConverter converter = new TestCurrencyConverter();
        Interval interval = Interval.of(LocalDate.parse("2009-12-31"), LocalDate.parse("2010-12-31"));

        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client, converter, interval);

        Money parentCost = snapshot.getRecord(parent).orElseThrow().getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED);
        Money spincoCost = snapshot.getRecord(spinco).orElseThrow().getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED);

        // conservation: 3,750 stays with parent + 1,250 derived to spinco == 5,000
        assertThat(parentCost, is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3750))));
        assertThat(spincoCost, is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1250))));
        assertThat(parentCost.add(spincoCost), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000))));
    }

    @Test
    public void testSharedSnapshotCacheServesMultipleIndependentSpinOffs()
    {
        Client client = new Client();

        Security parentX = new SecurityBuilder().addTo(client);
        Security spincoX = new SecurityBuilder().addTo(client);
        Security parentY = new SecurityBuilder().addTo(client);
        Security spincoY = new SecurityBuilder().addTo(client);

        // two independent parents in one portfolio -> two unrelated spin-offs
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(parentX, "2010-01-01", Values.Share.factorize(100), Values.Amount.factorize(4000)) //
                        .buy(parentY, "2010-01-01", Values.Share.factorize(100), Values.Amount.factorize(2000)) //
                        .addTo(client);

        addSpinOff(portfolio, parentX, spincoX, LocalDateTime.parse("2010-06-01T00:00"), new BigDecimal("0.25"));
        addSpinOff(portfolio, parentY, spincoY, LocalDateTime.parse("2010-06-01T00:00"), new BigDecimal("0.10"));

        CurrencyConverter converter = new TestCurrencyConverter();
        Interval interval = Interval.of(LocalDate.parse("2009-12-31"), LocalDate.parse("2010-12-31"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client, converter, interval);

        // one shared cache across all records; each event is single, so the
        // derived basis is exact and independent of record materialization order
        Money parentXcost = snapshot.getRecord(parentX).orElseThrow().getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED);
        Money spincoXcost = snapshot.getRecord(spincoX).orElseThrow().getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED);
        Money parentYcost = snapshot.getRecord(parentY).orElseThrow().getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED);
        Money spincoYcost = snapshot.getRecord(spincoY).orElseThrow().getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED);

        // event X: 25% of 4,000 = 1,000 to spinco; parent retains 3,000
        assertThat(spincoXcost, is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(parentXcost, is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3000))));
        assertThat(parentXcost.add(spincoXcost), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4000))));

        // event Y: 10% of 2,000 = 200 to spinco; parent retains 1,800
        assertThat(spincoYcost, is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200))));
        assertThat(parentYcost, is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1800))));
        assertThat(parentYcost.add(spincoYcost), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000))));
    }

    /**
     * Spec §4.2/§6: when the source security is foreign (source != term
     * currency), the parent's real {@code CostCalculation} pass computes the
     * basis in the term currency directly; Task 3.1's parent-first path in
     * {@link DistributionBasisCache#resolveViaParent} forces that real pass
     * to run on a cache miss, so the derived spinco basis is exact and
     * independent of record materialization order, with no ECB reference
     * rate re-entering the computation.
     */
    @Test
    public void testMultiCurrencyDerivedBasisIsExactAndOrderIndependent()
    {
        BigDecimal ratio = new BigDecimal("0.25");

        // snapshot A: spinco queried (materialised) first
        var a = buildMultiCurrencyFixture(ratio);
        LazySecurityPerformanceSnapshot snapshotA = LazySecurityPerformanceSnapshot.create(a.client(),
                        new TestCurrencyConverter(), mcInterval());
        Money spincoFirst = snapshotA.getRecord(a.spinco()).orElseThrow().getCost(CostMethod.FIFO,
                        TaxesAndFees.INCLUDED);
        Money parentAfter = snapshotA.getRecord(a.parent()).orElseThrow().getCost(CostMethod.FIFO,
                        TaxesAndFees.INCLUDED);

        // snapshot B: parent queried (materialised) first
        var b = buildMultiCurrencyFixture(ratio);
        LazySecurityPerformanceSnapshot snapshotB = LazySecurityPerformanceSnapshot.create(b.client(),
                        new TestCurrencyConverter(), mcInterval());
        Money parentFirst = snapshotB.getRecord(b.parent()).orElseThrow().getCost(CostMethod.FIFO,
                        TaxesAndFees.INCLUDED);
        Money spincoAfter = snapshotB.getRecord(b.spinco()).orElseThrow().getCost(CostMethod.FIFO,
                        TaxesAndFees.INCLUDED);

        // 25% of the EUR 4,000 parent basis derives to the spinco, exactly and
        // independent of record materialisation order; no ECB rate re-enters
        assertThat(spincoFirst, is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(spincoFirst, is(spincoAfter));
        assertThat(parentAfter, is(parentFirst));
        assertThat(parentFirst.add(spincoFirst), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4000))));
    }

    private static Interval mcInterval()
    {
        return Interval.of(LocalDate.parse("2015-01-01"), LocalDate.parse("2015-01-16"));
    }

    private static Fixture buildMultiCurrencyFixture(BigDecimal ratio)
    {
        Client client = new Client();

        // USD securities held in an EUR portfolio; the buy is booked in EUR
        // (account currency), so the parent basis is EUR 4,000
        Security parent = new SecurityBuilder(CurrencyUnit.USD).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.USD).addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(parent, "2015-01-02", Values.Share.factorize(100), Values.Amount.factorize(4000)) //
                        .addTo(client);

        addSpinOff(portfolio, parent, spinco, LocalDateTime.parse("2015-01-09T00:00"), ratio);

        return new Fixture(client, parent, spinco);
    }

    /**
     * KNOWN LIMITATION (deferred to Phase 3, spec §7.1): when the parent was
     * bought in multiple lots strictly before the reporting interval, its
     * opening position collapses into a single aggregated
     * {@code ValuationAtStart} lot for the parent's real
     * {@link CostCalculation} pass, which removes
     * {@code round(aggregate * ratio)}. The
     * {@link DistributionBasis#derivedInboundBasis} fallback sub-pass instead
     * reads the un-aggregated {@code portfolio.getTransactions()} and removes
     * {@code sum(round(lot * ratio))} across the individual lots. The two
     * disagree by a rounding cent and, because the snapshot-scoped
     * {@code DistributionBasisCache} memoizes whichever pass runs first, the
     * spinco's derived basis becomes dependent on which record is queried
     * first. This test asserts the desired, correct behavior -- exact
     * conservation and order-independence -- and is expected to fail until
     * Phase 3 reconciles the two passes.
     */
    @Test
    public void testPreIntervalMultiLotParentDerivedBasisIsExactAndOrderIndependent()
    {
        BigDecimal ratio = new BigDecimal("0.5");
        Money totalParentCost = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10001.02));

        // snapshot A: spinco record materializes before the parent record
        Fixture fixtureA = buildPreIntervalMultiLotFixture(ratio);
        LazySecurityPerformanceSnapshot snapshotA = LazySecurityPerformanceSnapshot.create(fixtureA.client(),
                        new TestCurrencyConverter(), interval());
        Money spincoCostSpincoFirst = snapshotA.getRecord(fixtureA.spinco()).orElseThrow()
                        .getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED);
        Money parentCostSpincoFirst = snapshotA.getRecord(fixtureA.parent()).orElseThrow()
                        .getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED);

        // snapshot B: parent record materializes before the spinco record
        Fixture fixtureB = buildPreIntervalMultiLotFixture(ratio);
        LazySecurityPerformanceSnapshot snapshotB = LazySecurityPerformanceSnapshot.create(fixtureB.client(),
                        new TestCurrencyConverter(), interval());
        Money parentCostParentFirst = snapshotB.getRecord(fixtureB.parent()).orElseThrow()
                        .getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED);
        Money spincoCostParentFirst = snapshotB.getRecord(fixtureB.spinco()).orElseThrow()
                        .getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED);

        // order-independence: the spinco's derived basis must not depend on
        // which record is queried (and therefore materialized) first
        assertThat(spincoCostSpincoFirst, is(spincoCostParentFirst));

        // exact conservation: what leaves the parent plus what the spinco
        // receives equals the parent's original total cost, in both orders
        assertThat(parentCostSpincoFirst.add(spincoCostSpincoFirst), is(totalParentCost));
        assertThat(parentCostParentFirst.add(spincoCostParentFirst), is(totalParentCost));
    }

    private static Interval interval()
    {
        return Interval.of(LocalDate.parse("2010-06-01"), LocalDate.parse("2010-12-31"));
    }

    private record Fixture(Client client, Security parent, Security spinco)
    {
    }

    private static Fixture buildPreIntervalMultiLotFixture(BigDecimal ratio)
    {
        Client client = new Client();

        Security parent = new SecurityBuilder().addTo(client);
        Security spinco = new SecurityBuilder().addTo(client);

        // two lots, both strictly before the interval start, so the parent's
        // opening position collapses into one aggregated ValuationAtStart lot
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(parent, "2010-01-01", Values.Share.factorize(50), Values.Amount.factorize(5000.51)) //
                        .buy(parent, "2010-02-01", Values.Share.factorize(50), Values.Amount.factorize(5000.51)) //
                        .addTo(client);

        // opening market value equals the opening cost (10,001.02 for 100
        // shares), so the aggregated real pass and the per-lot fallback pass
        // start from the same total and differ only in when the ratio is
        // rounded
        parent.addPrice(new SecurityPrice(LocalDate.parse("2010-06-01"), Values.Quote.factorize(100.0102)));

        addSpinOff(portfolio, parent, spinco, LocalDateTime.parse("2010-09-01T00:00"), ratio);

        return new Fixture(client, parent, spinco);
    }

    private static void addSpinOff(Portfolio portfolio, Security parent, Security spinco, LocalDateTime exDate,
                    BigDecimal basisRatio)
    {
        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setDateTime(exDate);
        source.setSecurity(parent);
        source.setShares(0);
        source.setCurrencyCode(CurrencyUnit.EUR);
        source.setAmount(Values.Amount.factorize(1));

        var target = new PortfolioTransaction();
        target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        target.setDateTime(exDate);
        target.setSecurity(spinco);
        target.setShares(Values.Share.factorize(10));
        target.setCurrencyCode(CurrencyUnit.EUR);
        target.setAmount(Values.Amount.factorize(1));

        CorporateActionEntry entry = new CorporateActionEntry();
        entry.setBasisRatio(basisRatio);
        entry.addLeg(portfolio, source, LegRole.SOURCE);
        entry.addLeg(portfolio, target, LegRole.TARGET);
        portfolio.addTransaction(source);
        portfolio.addTransaction(target);
    }
}
