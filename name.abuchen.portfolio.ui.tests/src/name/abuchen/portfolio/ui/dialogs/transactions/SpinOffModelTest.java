package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.junit.Test;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

public class SpinOffModelTest
{
    private SpinOffModel model(Client client)
    {
        return new SpinOffModel(client);
    }

    @Test
    public void testFloorSplitsEntitlementIntoWholeSharesAndFraction()
    {
        var model = model(new Client());

        // 33.333 spinco shares entitled -> 33 delivered, 0.333 cashed
        model.setEntitlement(Values.Share.factorize(33.333));

        assertThat(model.getWholeShares(), is(Values.Share.factorize(33)));
        assertThat(model.getFractionShares(), is(model.getEntitlement() - model.getWholeShares()));
        assertThat(model.getFractionShares() > 0, is(true));
    }

    @Test
    public void testWholeEntitlementHasNoFraction()
    {
        var model = model(new Client());
        model.setEntitlement(Values.Share.factorize(33));
        assertThat(model.getFractionShares(), is(0L));
    }

    @Test
    public void testFmvHelperComputesBasisRatio()
    {
        var model = model(new Client());

        // basis ratio = spinco FMV / (parent FMV + spinco FMV); shares carry it
        // e.g. parent 90.00, spinco 10.00 -> ratio 0.10
        model.setEntitlement(Values.Share.factorize(10));
        model.setParentFmvPerShare(Values.Quote.factorize(90));
        model.setSpinOffFmvPerShare(Values.Quote.factorize(10));
        model.applyFmvHelper();

        assertThat(model.getBasisRatio().compareTo(new BigDecimal("0.10")), is(0));
        // the spinco FMV/share is also the persisted reference price (§10)
        assertThat(model.getSpinOffPricePerShare(), is(Values.Quote.factorize(10)));
    }

    @Test
    public void testValidationRequiresTargetSecurityDistinctFromSource()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        var model = model(client);
        model.setSourceSecurity(parent);
        model.setTargetSecurity(parent); // same as source -> error
        model.setEntitlement(Values.Share.factorize(10));
        model.setBasisRatio(new BigDecimal("0.10"));

        assertThat(model.getCalculationStatus().getSeverity(), is(ValidationStatus.ERROR));
    }

    @Test
    public void testValidationOkForWellFormedSpinOff()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        var model = model(client);
        model.setSourceSecurity(parent);
        model.setTargetSecurity(spinco);
        model.setEntitlement(Values.Share.factorize(10));
        model.setBasisRatio(new BigDecimal("0.10"));
        model.setSpinOffPricePerShare(Values.Quote.factorize(10));

        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
    }

    @Test
    public void testApplyChangesBuildsAndRegistersFourLegGroup()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        Account reference = new Account("reference");
        reference.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(reference);
        client.addAccount(reference);
        client.addPortfolio(portfolio);

        var model = model(client);
        model.setSourceSecurity(parent);
        model.setPortfolio(portfolio);
        model.setExDate(java.time.LocalDate.parse("2015-01-09"));
        model.setTargetSecurity(spinco);
        model.setEntitlement(Values.Share.factorize(33.333)); // has a fraction
        model.setBasisRatio(new BigDecimal("0.25"));
        model.setSpinOffPricePerShare(Values.Quote.factorize(9));
        model.setCashInLieuAmount(Values.Amount.factorize(3));

        model.applyChanges();

        assertThat(client.getCorporateActions(), hasSize(1));
        CorporateActionEntry entry = client.getCorporateActions().get(0);
        assertThat(entry.getLegs(), hasSize(4)); // fraction present

        var source = entry.getLeg(CorporateActionEntry.LegRole.SOURCE).orElseThrow();
        var target = entry.getLeg(CorporateActionEntry.LegRole.TARGET).orElseThrow();
        assertThat(source.getShares(), is(0L)); // §3 source shares == 0
        assertThat(target.getShares(), is(Values.Share.factorize(33.333)));
        // both mandatory legs are on the portfolio; cash-in-lieu on the reference account
        assertThat(portfolio.getTransactions().size(), is(3));
        assertThat(reference.getTransactions().size(), is(1));
    }

    @Test
    public void testWholeEntitlementProducesTwoLegsOnly()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        Account reference = new Account("reference");
        reference.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(reference);
        client.addAccount(reference);
        client.addPortfolio(portfolio);

        var model = model(client);
        model.setSourceSecurity(parent);
        model.setPortfolio(portfolio);
        model.setTargetSecurity(spinco);
        model.setEntitlement(Values.Share.factorize(33)); // whole -> no fraction legs
        model.setBasisRatio(new BigDecimal("0.25"));

        model.applyChanges();

        CorporateActionEntry entry = client.getCorporateActions().get(0);
        assertThat(entry.getLegs(), hasSize(2));
        assertThat(reference.getTransactions().size(), is(0));
    }

    @Test
    public void testCreateNewTargetSecurityIsAddedToClient()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        Account reference = new Account("reference");
        reference.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(reference);
        client.addAccount(reference);
        client.addPortfolio(portfolio);

        int before = client.getSecurities().size();

        var model = model(client);
        model.setSourceSecurity(parent);
        model.setPortfolio(portfolio);
        // a transient security not yet added to the client
        Security spinco = new Security("Spinco", CurrencyUnit.EUR);
        model.setTargetSecurity(spinco);
        model.setEntitlement(Values.Share.factorize(10));
        model.setBasisRatio(new BigDecimal("0.10"));

        model.applyChanges();

        assertThat(client.getSecurities().size(), is(before + 1));
        assertThat(client.getSecurities().contains(spinco), is(true));
    }

    @Test
    public void testMultiCurrencyEditRoundTripRestoresExchangeRate()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.USD).addTo(client);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        Account reference = new Account("reference");
        reference.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(reference);
        client.addAccount(reference);
        client.addPortfolio(portfolio);

        // create mode: whole entitlement (no fraction/cash-in-lieu legs) so the
        // test stays focused on the target leg's forex unit
        var createModel = model(client);
        createModel.setSourceSecurity(parent);
        createModel.setPortfolio(portfolio);
        createModel.setTargetSecurity(spinco);
        createModel.setEntitlement(Values.Share.factorize(10));
        createModel.setSpinOffPricePerShare(Values.Quote.factorize(10));
        createModel.setBasisRatio(new BigDecimal("0.25"));
        createModel.setExchangeRate(new BigDecimal("1.10"));

        createModel.applyChanges();

        assertThat(client.getCorporateActions(), hasSize(1));
        CorporateActionEntry entry = client.getCorporateActions().get(0);
        var target = entry.getLeg(CorporateActionEntry.LegRole.TARGET).orElseThrow();
        var unit = target.getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(unit.getExchangeRate().compareTo(new BigDecimal("1.10")), is(0));

        // edit mode: prefill from the existing group, change the entitlement,
        // and re-apply; before the fix this threw IllegalArgumentException
        // (exchangeRate defaulted to ONE) after already deleting the group
        var editModel = model(client);
        editModel.setSource(entry);
        editModel.setEntitlement(Values.Share.factorize(12));

        editModel.applyChanges();

        assertThat(client.getCorporateActions(), hasSize(1));
        CorporateActionEntry updated = client.getCorporateActions().get(0);
        var updatedTarget = updated.getLeg(CorporateActionEntry.LegRole.TARGET).orElseThrow();
        assertThat(updatedTarget.getShares(), is(Values.Share.factorize(12)));
        var updatedUnit = updatedTarget
                        .getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(updatedUnit.getExchangeRate().compareTo(new BigDecimal("1.10")), is(0));
    }

    @Test
    public void testBuiltFractionLegsHaveSellTypeAndCarryTargetSecurity()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        Account reference = new Account("reference");
        reference.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setReferenceAccount(reference);
        client.addAccount(reference);
        client.addPortfolio(portfolio);

        var model = model(client);
        model.setSourceSecurity(parent);
        model.setPortfolio(portfolio);
        model.setExDate(java.time.LocalDate.parse("2015-01-09"));
        model.setTargetSecurity(spinco);
        model.setEntitlement(Values.Share.factorize(33.333)); // has a fraction
        model.setBasisRatio(new BigDecimal("0.25"));
        model.setSpinOffPricePerShare(Values.Quote.factorize(9));
        model.setCashInLieuAmount(Values.Amount.factorize(3));

        model.applyChanges();

        CorporateActionEntry entry = client.getCorporateActions().get(0);
        var fraction = entry.getLeg(CorporateActionEntry.LegRole.FRACTION_SALE).orElseThrow();
        var cash = entry.getLeg(CorporateActionEntry.LegRole.CASH_IN_LIEU).orElseThrow();
        assertThat(((PortfolioTransaction) fraction).getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(((AccountTransaction) cash).getType(), is(AccountTransaction.Type.SELL));
        assertThat(fraction.getSecurity(),
                        is(entry.getLeg(CorporateActionEntry.LegRole.TARGET).orElseThrow().getSecurity()));
    }

    @Test
    public void testFractionalEntitlementWithoutReferenceAccountIsInvalid()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        // no reference account set
        client.addPortfolio(portfolio);

        var model = model(client);
        model.setSourceSecurity(parent);
        model.setPortfolio(portfolio);
        model.setTargetSecurity(spinco);
        model.setEntitlement(Values.Share.factorize(33.333)); // has a fraction
        model.setBasisRatio(new BigDecimal("0.25"));

        assertThat(model.getCalculationStatus().getSeverity(), is(ValidationStatus.ERROR));

        // a whole entitlement (no fraction) is fine on the same account-less
        // portfolio -> proves the guard is fraction-scoped
        model.setEntitlement(Values.Share.factorize(33));

        assertThat(model.getCalculationStatus(), is(ValidationStatus.ok()));
    }

    @Test
    public void testApplyChangesDoesNotDeleteGroupWhenReferenceAccountMissing()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        // no reference account -- create a whole-share entry (no fraction,
        // so no CASH_IN_LIEU leg/owner is captured on a later edit either)
        client.addPortfolio(portfolio);

        var createModel = model(client);
        createModel.setSourceSecurity(parent);
        createModel.setPortfolio(portfolio);
        createModel.setTargetSecurity(spinco);
        createModel.setEntitlement(Values.Share.factorize(33)); // whole -> no fraction legs
        createModel.setBasisRatio(new BigDecimal("0.25"));

        createModel.applyChanges();

        assertThat(client.getCorporateActions(), hasSize(1));
        CorporateActionEntry entry = client.getCorporateActions().get(0);

        // simulate an edit that introduces a fraction while there is still
        // no reference account (and, since the original group had no
        // CASH_IN_LIEU leg, no cash-in-lieu owner was captured either --
        // resolveCashAccount() has nothing to fall back on)
        var editModel = model(client);
        editModel.setSource(entry);
        editModel.setEntitlement(Values.Share.factorize(33.333)); // now fractional

        assertThrows(UnsupportedOperationException.class, editModel::applyChanges);

        // the original group must survive -- no destructive delete happened
        assertThat(client.getCorporateActions(), hasSize(1));
        assertThat(client.getCorporateActions().get(0), is(entry));
    }

    // ---- ratio -> entitlement seeding -----------------------------------

    /** A model whose holdings-at-ex-date is stubbed, so seeding logic is
     *  testable without a live PortfolioSnapshot. */
    private static class StubModel extends SpinOffModel
    {
        long stubHoldings;

        StubModel(Client client, long stubHoldings)
        {
            super(client);
            this.stubHoldings = stubHoldings;
        }

        @Override
        long sharesHeldAtExDate()
        {
            return stubHoldings;
        }
    }

    @Test
    public void testRatioSeedsEntitlementFromHoldings()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Portfolio portfolio = new Portfolio();
        client.addPortfolio(portfolio);

        var model = new StubModel(client, Values.Share.factorize(1000));
        model.setSourceSecurity(parent);
        model.setTargetSecurity(spinco);
        model.setPortfolio(portfolio);
        model.setExDate(java.time.LocalDate.parse("2025-10-20"));

        // 1 new share for every 20 held -> 1000 / 20 = 50
        model.setDistributionNumerator(1);
        model.setDistributionDenominator(20);

        assertThat(model.getEntitlement(), is(Values.Share.factorize(50)));
    }

    @Test
    public void testManualEntitlementOverrideSticksUntilRatioChanges()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Portfolio portfolio = new Portfolio();
        client.addPortfolio(portfolio);

        var model = new StubModel(client, Values.Share.factorize(1000));
        model.setSourceSecurity(parent);
        model.setTargetSecurity(spinco);
        model.setPortfolio(portfolio);
        model.setExDate(java.time.LocalDate.parse("2025-10-20"));
        model.setDistributionNumerator(1);
        model.setDistributionDenominator(20); // seeds 50

        // user override sticks (no trigger fired)
        model.setEntitlement(Values.Share.factorize(48));
        assertThat(model.getEntitlement(), is(Values.Share.factorize(48)));

        // changing the ratio reseeds
        model.setDistributionDenominator(10); // 1000 / 10 = 100
        assertThat(model.getEntitlement(), is(Values.Share.factorize(100)));
    }

    @Test
    public void testZeroDenominatorDoesNotSeedOrThrow()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Portfolio portfolio = new Portfolio();
        client.addPortfolio(portfolio);

        var model = new StubModel(client, Values.Share.factorize(1000));

        // set the zero denominator before the other seed triggers so the
        // default 1/1 ratio never gets a chance to seed first (§10 reseeds
        // on every ratio/portfolio/source/ex-date change using the *current*
        // ratio; asserting this guard in isolation requires the zero
        // denominator to already be in place)
        model.setDistributionNumerator(1);
        model.setDistributionDenominator(0); // must not divide by zero, must not seed

        model.setSourceSecurity(parent);
        model.setPortfolio(portfolio);
        model.setExDate(java.time.LocalDate.parse("2025-10-20"));

        assertThat(model.getEntitlement(), is(0L));
    }

    @Test
    public void testEditModePrefillDoesNotReseedButRatioChangeDoes()
    {
        // build a stored four-leg group with 33.333 target shares, ratio 1/1
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Portfolio portfolio = new Portfolio();
        client.addPortfolio(portfolio);
        Account account = new Account("a");
        client.addAccount(account);

        var exDate = java.time.LocalDateTime.parse("2015-01-09T00:00");
        var src = new PortfolioTransaction();
        src.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        src.setDateTime(exDate);
        src.setSecurity(parent);
        src.setCurrencyCode(CurrencyUnit.EUR);
        var tgt = new PortfolioTransaction();
        tgt.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        tgt.setDateTime(exDate);
        tgt.setSecurity(spinco);
        tgt.setCurrencyCode(CurrencyUnit.EUR);
        tgt.setShares(Values.Share.factorize(33.333));

        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.setExDate(exDate.toLocalDate());
        entry.setBasisRatio(new BigDecimal("0.25"));
        entry.setDistributionRatio(new CorporateActionEntry.DistributionRatio(1, 1));
        entry.addLeg(portfolio, src, CorporateActionEntry.LegRole.SOURCE);
        entry.addLeg(portfolio, tgt, CorporateActionEntry.LegRole.TARGET);
        entry.insert();
        client.addCorporateAction(entry);

        var model = new StubModel(client, Values.Share.factorize(1000));
        model.setSource(entry);

        // prefill preserves the stored fact (not reseeded to 1000 x 1/1)
        assertThat(model.getEntitlement(), is(Values.Share.factorize(33.333)));

        // a non-ratio change in edit mode does NOT reseed (§11)
        model.setExDate(java.time.LocalDate.parse("2015-02-01"));
        assertThat(model.getEntitlement(), is(Values.Share.factorize(33.333)));

        // but changing the ratio does reseed (1000 x 1/20 = 50)
        model.setDistributionDenominator(20);
        assertThat(model.getEntitlement(), is(Values.Share.factorize(50)));
    }

    @Test
    public void testEditPreservesNonReferenceCashInLieuAccount()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Portfolio portfolio = new Portfolio();
        client.addPortfolio(portfolio);
        Account reference = new Account("reference");
        client.addAccount(reference);
        portfolio.setReferenceAccount(reference);
        Account other = new Account("other-cash");
        client.addAccount(other);

        var exDate = java.time.LocalDateTime.parse("2015-01-09T00:00");
        var src = new PortfolioTransaction();
        src.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        src.setDateTime(exDate); src.setSecurity(parent); src.setCurrencyCode(CurrencyUnit.EUR);
        var tgt = new PortfolioTransaction();
        tgt.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        tgt.setDateTime(exDate); tgt.setSecurity(spinco); tgt.setCurrencyCode(CurrencyUnit.EUR);
        tgt.setShares(Values.Share.factorize(10.5));
        var frac = new PortfolioTransaction();
        frac.setType(PortfolioTransaction.Type.SELL);
        frac.setDateTime(exDate); frac.setSecurity(spinco); frac.setCurrencyCode(CurrencyUnit.EUR);
        frac.setShares(Values.Share.factorize(0.5));
        var cash = new AccountTransaction();
        cash.setType(AccountTransaction.Type.SELL);
        cash.setDateTime(exDate); cash.setSecurity(spinco); cash.setCurrencyCode(CurrencyUnit.EUR);

        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.setExDate(exDate.toLocalDate());
        entry.setBasisRatio(new BigDecimal("0.25"));
        entry.setDistributionRatio(new CorporateActionEntry.DistributionRatio(1, 1));
        entry.setReferencePrice(Values.Quote.factorize(5));
        entry.addLeg(portfolio, src, CorporateActionEntry.LegRole.SOURCE);
        entry.addLeg(portfolio, tgt, CorporateActionEntry.LegRole.TARGET);
        entry.addLeg(portfolio, frac, CorporateActionEntry.LegRole.FRACTION_SALE);
        entry.addLeg(other, cash, CorporateActionEntry.LegRole.CASH_IN_LIEU); // NOT the reference account
        entry.insert();
        client.addCorporateAction(entry);

        var model = new SpinOffModel(client);
        model.setSource(entry);
        model.applyChanges();

        var rebuilt = client.getCorporateActions().get(0);
        var cashOwner = rebuilt.getLegs().stream()
                        .filter(l -> l.role() == CorporateActionEntry.LegRole.CASH_IN_LIEU).findFirst()
                        .map(CorporateActionEntry.Leg::owner).orElseThrow();
        assertThat(cashOwner, is((Object) other)); // preserved, not relocated to `reference`
    }

    // ---- entry-time validation guards (A2/A3/A4) -------------------------

    private SpinOffModel modelWithFraction(Client client, Account cashAccount, String targetCurrency,
                    String cashCurrency)
    {
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(targetCurrency).addTo(client);
        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(cashAccount);
        client.addPortfolio(portfolio);

        var model = new SpinOffModel(client);
        model.setSourceSecurity(parent);
        model.setTargetSecurity(spinco);
        model.setPortfolio(portfolio);
        model.setExDate(java.time.LocalDate.parse("2025-10-20"));
        model.setBasisRatio(new BigDecimal("0.25"));
        model.setEntitlement(Values.Share.factorize(10.5)); // fraction 0.5 -> cash leg needed
        return model;
    }

    @Test
    public void testZeroExchangeRateWhenCurrenciesDifferIsError()
    {
        Client client = new Client();
        Account cash = new Account("c"); cash.setCurrencyCode(CurrencyUnit.USD); client.addAccount(cash);
        var model = modelWithFraction(client, cash, CurrencyUnit.USD, CurrencyUnit.USD); // target USD != source EUR
        model.setExchangeRate(BigDecimal.ZERO);
        assertThat(model.getCalculationStatus().getSeverity(), is(org.eclipse.core.runtime.IStatus.ERROR));
        model.setExchangeRate(new BigDecimal("1.10"));
        assertThat(model.getCalculationStatus().getSeverity(), is(org.eclipse.core.runtime.IStatus.OK));
    }

    @Test
    public void testThirdCurrencyCashAccountIsError()
    {
        Client client = new Client();
        Account cash = new Account("c"); cash.setCurrencyCode("GBP"); client.addAccount(cash);
        // source EUR, target USD, cash GBP -> unsupported third currency
        var model = modelWithFraction(client, cash, CurrencyUnit.USD, "GBP");
        model.setExchangeRate(new BigDecimal("1.10"));
        assertThat(model.getCalculationStatus().getSeverity(), is(org.eclipse.core.runtime.IStatus.ERROR));
    }

    @Test
    public void testNonPositiveRatioIsError()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        var model = new SpinOffModel(client);
        model.setSourceSecurity(parent);
        model.setTargetSecurity(spinco);
        model.setBasisRatio(new BigDecimal("0.25"));
        model.setEntitlement(Values.Share.factorize(50));
        model.setDistributionDenominator(0);
        assertThat(model.getCalculationStatus().getSeverity(), is(org.eclipse.core.runtime.IStatus.ERROR));
    }

    // ---- §13.2 entry-time soft-warn (A5) ---------------------------------

    @Test
    public void testEntitlementWarningIsNonGatingAndTracksDivergence()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Portfolio portfolio = new Portfolio();
        client.addPortfolio(portfolio);

        var model = new StubModel(client, Values.Share.factorize(1000));
        model.setSourceSecurity(parent);
        model.setTargetSecurity(spinco);
        model.setPortfolio(portfolio);
        model.setExDate(java.time.LocalDate.parse("2025-10-20"));
        model.setBasisRatio(new BigDecimal("0.25"));
        model.setDistributionNumerator(1);
        model.setDistributionDenominator(20); // seeds entitlement = 50 -> matches, no warning

        assertThat(model.getEntitlementWarning(), is((String) null));

        // a manual override that diverges from ratio x holdings warns, but does NOT gate OK
        model.setEntitlement(Values.Share.factorize(48));
        assertThat(model.getEntitlementWarning(), is(Messages.MsgSpinOffEntitlementDiffersFromRatio));
        assertThat(model.getCalculationStatus().getSeverity(), is(org.eclipse.core.runtime.IStatus.OK));
    }
}
