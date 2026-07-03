package name.abuchen.portfolio.checks.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.checks.impl.SpinOffConsistencyCheck.Defect;
import name.abuchen.portfolio.checks.impl.SpinOffConsistencyCheck.SpinOffIssue;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.CorporateActionEntry.LegRole;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SpinOffConsistencyCheckTest
{
    // ---- fixture ---------------------------------------------------------

    private static class Fixture
    {
        Client client;
        Portfolio portfolio;
        Account account;
        Security parent;
        Security spinco;
        CorporateActionEntry entry;
        PortfolioTransaction source;
        PortfolioTransaction target;
        PortfolioTransaction fractionSale;
        AccountTransaction cashInLieu;
    }

    /** A structurally-clean four-leg EUR spin-off with a valid basis ratio. */
    private Fixture validFourLeg()
    {
        Fixture f = new Fixture();
        f.client = new Client();
        f.parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(f.client);
        f.spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(f.client);

        f.portfolio = new Portfolio();
        f.portfolio.setName("portfolio");
        f.client.addPortfolio(f.portfolio);
        f.account = new Account("account");
        f.client.addAccount(f.account);

        var exDate = LocalDateTime.parse("2015-01-09T00:00");

        f.source = new PortfolioTransaction();
        f.source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        f.source.setDateTime(exDate);
        f.source.setSecurity(f.parent);
        f.source.setCurrencyCode(CurrencyUnit.EUR);

        f.target = new PortfolioTransaction();
        f.target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        f.target.setDateTime(exDate);
        f.target.setSecurity(f.spinco);
        f.target.setCurrencyCode(CurrencyUnit.EUR);
        f.target.setShares(Values.Share.factorize(33.333));

        f.fractionSale = new PortfolioTransaction();
        f.fractionSale.setType(PortfolioTransaction.Type.SELL);
        f.fractionSale.setDateTime(exDate);
        f.fractionSale.setSecurity(f.spinco);
        f.fractionSale.setCurrencyCode(CurrencyUnit.EUR);

        f.cashInLieu = new AccountTransaction();
        f.cashInLieu.setType(AccountTransaction.Type.SELL);
        f.cashInLieu.setDateTime(exDate);
        f.cashInLieu.setSecurity(f.spinco);
        f.cashInLieu.setCurrencyCode(CurrencyUnit.EUR);

        f.entry = new CorporateActionEntry();
        f.entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        f.entry.setExDate(exDate.toLocalDate());
        f.entry.setBasisRatio(new java.math.BigDecimal("0.25"));
        f.entry.setDistributionRatio(new CorporateActionEntry.DistributionRatio(1, 1));
        f.entry.addLeg(f.portfolio, f.source, LegRole.SOURCE);
        f.entry.addLeg(f.portfolio, f.target, LegRole.TARGET);
        f.entry.addLeg(f.portfolio, f.fractionSale, LegRole.FRACTION_SALE);
        f.entry.addLeg(f.account, f.cashInLieu, LegRole.CASH_IN_LIEU);
        f.entry.insert();
        f.client.addCorporateAction(f.entry);
        return f;
    }

    private List<Issue> run(Client client)
    {
        return new SpinOffConsistencyCheck().execute(client);
    }

    private Defect onlyDefect(List<Issue> issues)
    {
        assertThat(issues, hasSize(1));
        return ((SpinOffIssue) issues.get(0)).getDefect();
    }

    // ---- happy path ------------------------------------------------------

    @Test
    public void testEmptyClientHasNoIssues()
    {
        assertThat(run(new Client()), hasSize(0));
    }

    @Test
    public void testValidFourLegHasNoIssues()
    {
        assertThat(run(validFourLeg().client), hasSize(0));
    }

    // ---- structural defects ---------------------------------------------

    @Test
    public void testMissingTargetLegIsReported()
    {
        // rebuild an entry without the TARGET leg
        Fixture f = validFourLeg();
        f.client.removeCorporateAction(f.entry);
        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.setBasisRatio(new java.math.BigDecimal("0.25"));
        entry.addLeg(f.portfolio, f.source, LegRole.SOURCE);
        f.client.addCorporateAction(entry);

        assertThat(onlyDefect(run(f.client)), is(Defect.MISSING_LEG));
    }

    @Test
    public void testUnpairedFractionSaleIsReported()
    {
        // fraction sale present, cash-in-lieu absent
        Fixture f = validFourLeg();
        f.client.removeCorporateAction(f.entry);
        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.setBasisRatio(new java.math.BigDecimal("0.25"));
        entry.addLeg(f.portfolio, f.source, LegRole.SOURCE);
        entry.addLeg(f.portfolio, f.target, LegRole.TARGET);
        entry.addLeg(f.portfolio, f.fractionSale, LegRole.FRACTION_SALE);
        f.client.addCorporateAction(entry);

        assertThat(onlyDefect(run(f.client)), is(Defect.UNPAIRED_FRACTION));
    }

    @Test
    public void testDuplicateSourceLegIsReported()
    {
        Fixture f = validFourLeg();
        var extraSource = new PortfolioTransaction();
        extraSource.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        extraSource.setSecurity(f.parent);
        extraSource.setCurrencyCode(CurrencyUnit.EUR);
        f.entry.addLeg(f.portfolio, extraSource, LegRole.SOURCE);

        assertThat(onlyDefect(run(f.client)), is(Defect.DUPLICATE_LEG));
    }

    // ---- the offer-to-fix ------------------------------------------------

    @Test
    public void testDeleteFixRemovesTheWholeGroup()
    {
        // a broken group (missing TARGET) whose only leg is the source; the
        // scoped fix removes that leg from the owner and drops the registry
        // entry
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        client.addPortfolio(portfolio);

        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setSecurity(parent);
        source.setCurrencyCode(CurrencyUnit.EUR);

        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.setBasisRatio(new java.math.BigDecimal("0.25"));
        entry.addLeg(portfolio, source, LegRole.SOURCE); // missing TARGET
        entry.insert();
        client.addCorporateAction(entry);

        List<Issue> issues = run(client);
        assertThat(issues, hasSize(1));
        List<QuickFix> fixes = issues.get(0).getAvailableFixes();
        assertThat(fixes.isEmpty(), is(false));
        fixes.get(0).execute();

        // group gone from the registry AND the leg removed from its owner
        assertThat(client.getCorporateActions(), hasSize(0));
        assertThat(portfolio.getTransactions(), hasSize(0));
        // re-running finds nothing
        assertThat(run(client), hasSize(0));
    }

    @Test
    public void testDeleteFixOnFourLegGroupClearsBothOwners()
    {
        Fixture f = validFourLeg();
        // make it defective (duplicate) so an issue+fix is produced
        f.entry.addLeg(f.portfolio, f.source, LegRole.SOURCE);

        List<Issue> issues = run(f.client);
        issues.get(0).getAvailableFixes().get(0).execute();

        assertThat(f.client.getCorporateActions(), hasSize(0));
        assertThat(f.portfolio.getTransactions(), hasSize(0));
        assertThat(f.account.getTransactions(), hasSize(0));
    }

    // ---- severed legs ---------------------------------------------------

    @Test
    public void testSeveredLegRemovedFromOwnerIsReported()
    {
        Fixture f = validFourLeg();
        // sever the TARGET leg from its owner without touching the entry's leg
        // list (simulates an externally-edited file / a broken group)
        f.portfolio.getTransactions().remove(f.target);

        assertThat(onlyDefect(run(f.client)), is(Defect.SEVERED_LEG));
    }

    @Test
    public void testLegWithForeignCrossEntryIsReportedAsSevered()
    {
        Fixture f = validFourLeg();
        // repoint the SOURCE leg's back-pointer at an unrelated entry
        var other = new CorporateActionEntry();
        other.setType(CorporateActionEntry.Type.SPIN_OFF);
        other.addLeg(f.portfolio, f.source, LegRole.SOURCE); // setCrossEntry(other)

        assertThat(onlyDefect(run(f.client)), is(Defect.SEVERED_LEG));
    }

    // ---- basis ratio range ----------------------------------------------

    @Test
    public void testBasisRatioBelowRangeIsReported()
    {
        Fixture f = validFourLeg();
        f.entry.setBasisRatio(java.math.BigDecimal.ZERO);
        assertThat(onlyDefect(run(f.client)), is(Defect.BASIS_RATIO_OUT_OF_RANGE));
    }

    @Test
    public void testBasisRatioAtOrAboveOneIsReported()
    {
        Fixture f = validFourLeg();
        f.entry.setBasisRatio(java.math.BigDecimal.ONE);
        assertThat(onlyDefect(run(f.client)), is(Defect.BASIS_RATIO_OUT_OF_RANGE));
    }

    @Test
    public void testNullBasisRatioIsReported()
    {
        Fixture f = validFourLeg();
        f.entry.setBasisRatio(null);
        assertThat(onlyDefect(run(f.client)), is(Defect.BASIS_RATIO_OUT_OF_RANGE));
    }

    // ---- currency mismatch ----------------------------------------------

    @Test
    public void testCurrencyMismatchBetweenSourceAndTargetIsReported()
    {
        Fixture f = validFourLeg();
        // event currency = source security (EUR); target leg denominated in USD
        // breaks the §6 "both mandatory legs share the source currency" rule
        f.target.setCurrencyCode(CurrencyUnit.USD);
        assertThat(onlyDefect(run(f.client)), is(Defect.CURRENCY_MISMATCH));
    }

    @Test
    public void testSourceLegCurrencyNotMatchingItsSecurityIsReported()
    {
        Fixture f = validFourLeg();
        // source security is EUR but the source leg is denominated USD (the
        // source leg carries no forex; its currency must equal its security's)
        f.source.setCurrencyCode(CurrencyUnit.USD);
        assertThat(onlyDefect(run(f.client)), is(Defect.CURRENCY_MISMATCH));
    }

    // ---- 1/1 distribution ratio is not authoritative -------------------

    @Test
    public void testPlaceholderDistributionRatioIsNotTreatedAsAuthoritative()
    {
        // the dialog persists DistributionRatio(1,1); entitlement is entered
        // directly and is NOT ratio x holdings. The check must NOT flag this.
        Fixture f = validFourLeg();
        f.entry.setDistributionRatio(new CorporateActionEntry.DistributionRatio(1, 1));
        f.target.setShares(Values.Share.factorize(33.333)); // != 1 x anything
        assertThat(run(f.client), hasSize(0));
    }
}
