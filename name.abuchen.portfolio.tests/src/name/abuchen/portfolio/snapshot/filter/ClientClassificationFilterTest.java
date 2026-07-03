package name.abuchen.portfolio.snapshot.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.CorporateActionEntry.LegRole;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class ClientClassificationFilterTest
{
    @Test
    public void testThatFilteringThenDerivingCostOnSpinOffDoesNotThrow()
    {
        Client spinOffClient = new Client();

        Security parent = new SecurityBuilder().addTo(spinOffClient);
        Security spinco = new SecurityBuilder().addTo(spinOffClient);

        // 100 parent shares, cost basis 5,000.00
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(parent, "2010-01-01", Values.Share.factorize(100), Values.Amount.factorize(5000)) //
                        .addTo(spinOffClient);

        var exDate = LocalDateTime.parse("2010-06-01T00:00");

        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setDateTime(exDate);
        source.setSecurity(parent);
        source.setShares(0);
        source.setCurrencyCode(CurrencyUnit.EUR);
        source.setAmount(Values.Amount.factorize(2000));

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

        // classify the reference account and both securities so that
        // ClientClassificationFilter keeps every transaction at full (100%)
        // weight -- this isolates the effect under test (the missing
        // CorporateActionEntry cross-entry on the filtered/copied legs) from
        // any partial-weight amount adjustments
        var taxonomy = new Taxonomy("Test Taxonomy");
        var root = new Classification("root", "Root");
        taxonomy.setRootNode(root);

        var classification = new Classification(root, "all", "All");
        root.addChild(classification);
        classification.addAssignment(new Classification.Assignment(portfolio.getReferenceAccount()));
        classification.addAssignment(new Classification.Assignment(parent));
        classification.addAssignment(new Classification.Assignment(spinco));

        spinOffClient.addTaxonomy(taxonomy);

        CurrencyConverter converter = new TestCurrencyConverter();
        Interval interval = Interval.of(LocalDate.parse("2009-12-31"), LocalDate.parse("2010-12-31"));

        // sanity check: on the un-filtered client, basis derivation works as
        // in SpinOffBasisDerivationTest (3,750 stays with parent, 1,250 is
        // derived to spinco)
        LazySecurityPerformanceSnapshot unfiltered = LazySecurityPerformanceSnapshot.create(spinOffClient, converter,
                        interval);
        assertThat(unfiltered.getRecord(parent).orElseThrow().getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3750))));
        assertThat(unfiltered.getRecord(spinco).orElseThrow().getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1250))));

        // code under test: ClientClassificationFilter copies the
        // distribution legs without a CorporateActionEntry cross-entry link
        // (the original entry references the un-filtered legs), so basis
        // derivation on the filtered client must not throw
        // IllegalStateException; instead it must fall back to a tolerant
        // no-op (no reduction on the source, a 0-basis lot on the target)
        Client filtered = new ClientClassificationFilter(classification).filter(spinOffClient);

        LazySecurityPerformanceSnapshot filteredSnapshot = LazySecurityPerformanceSnapshot.create(filtered, converter,
                        interval);

        assertThat(filteredSnapshot.getRecord(parent).orElseThrow().getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000))));
        assertThat(filteredSnapshot.getRecord(spinco).orElseThrow().getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(filteredSnapshot.getRecord(spinco).orElseThrow().getSharesHeld(), is(Values.Share.factorize(100)));
    }
}
