package name.abuchen.portfolio.model.ledger.nativeentry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionBasisMethod;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionBasisStatus;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LedgerCorporateActionFluentApiTest
{
    private static final LocalDateTime DATE = LocalDateTime.of(2020, 9, 28, 0, 0);

    @Test
    public void testCreatesCashDistribution()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.CASH_DISTRIBUTION) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .cash("cash-1", fixture.account, money(15)) //
                        .fee("fee-1", fixture.account, money(2), "cash-1") //
                        .tax("tax-1", fixture.account, money(1), "cash-1") //
                        .buildDetached().getEntry();

        assertThat(entry.getPostings().size(), is(4));
        assertThat(posting(entry, LedgerLegRole.CASH_LEG, "cash-1").getAmount(), is(money(15).getAmount()));
    }

    @Test
    public void testCreatesCouponPaymentWithSameGroupInterestDetail()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.COUPON_PAYMENT) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .cash("cash-1", fixture.account, money(12)) //
                        .accruedInterest("interest-1", "cash-1", fixture.account, money(12)) //
                        .buildDetached().getEntry();

        assertThat(posting(entry, LedgerLegRole.CASH_LEG, "cash-1").getGroupKey(), is("cash-1"));
        assertThat(posting(entry, LedgerLegRole.ACCRUED_INTEREST_LEG, "interest-1").getGroupKey(), is("cash-1"));
    }

    @Test
    public void testCouponPaymentDifferentGroupInterestFailsValidation()
    {
        var fixture = fixture();
        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                                        .kind(CorporateActionKind.COUPON_PAYMENT) //
                                        .date(DATE) //
                                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                                        .cash("cash-1", fixture.account, money(12)) //
                                        .accruedInterest("interest-1", "other", fixture.account, money(12)) //
                                        .buildDetached());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.NATIVE_DEFINITION_VALIDATION_FAILED));
    }

    @Test
    public void testCreatesSpinOffWithRepeatedTargetsAndBasis()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.SPIN_OFF) //
                        .date(DATE) //
                        .effectiveDate(LocalDate.of(2020, 9, 28)) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.target, Values.Share.factorize(5)) //
                        .securityIn("target-2", "main", fixture.portfolio, fixture.secondTarget,
                                        Values.Share.factorize(2)) //
                        .cash("cash-1", fixture.account, money(3)) //
                        .basis(CorporateActionBasisStatus.PROVIDED) //
                        .basisMethod(CorporateActionBasisMethod.PERCENTAGE_ALLOCATION) //
                        .basisPercentageAllocation(LedgerLegRole.SECURITY_CONTEXT_LEG, "context-1", "main",
                                        new BigDecimal("75")) //
                        .basisPercentageAllocation(LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main",
                                        new BigDecimal("20")) //
                        .basisPercentageAllocation(LedgerLegRole.TARGET_SECURITY_LEG, "target-2", "main",
                                        new BigDecimal("5")) //
                        .buildDetached().getEntry();

        assertThat(posting(entry, LedgerLegRole.TARGET_SECURITY_LEG, "target-2").getSecurity(),
                        is(fixture.secondTarget));
        assertThat(entry.getPostings().stream().filter(posting -> posting.getLocalKey().startsWith("target-"))
                        .count(), is(2L));
    }

    @Test
    public void testCreatesConversionWithBasis()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.CONVERSION) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .securityOut("source-1", "main", fixture.portfolio, fixture.source,
                                        Values.Share.factorize(10)) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.target,
                                        Values.Share.factorize(5)) //
                        .basis(CorporateActionBasisStatus.PROVIDED) //
                        .basisMethod(CorporateActionBasisMethod.PERCENTAGE_ALLOCATION) //
                        .basisPercentageAllocation(LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main",
                                        new BigDecimal("100")) //
                        .buildDetached().getEntry();

        assertThat(posting(entry, LedgerLegRole.SOURCE_SECURITY_LEG, "source-1").getShares(),
                        is(Values.Share.factorize(10)));
    }

    @Test
    public void testCreatesMaturityWithPrincipal()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.MATURITY) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .securityOut("source-1", "main", fixture.portfolio, fixture.source,
                                        Values.Share.factorize(10)) //
                        .cash("cash-1", fixture.account, money(100)) //
                        .principal("principal-1", fixture.account, money(100)) //
                        .buildDetached().getEntry();

        assertThat(posting(entry, LedgerLegRole.PRINCIPAL_REDEMPTION_LEG, "principal-1").getAmount(),
                        is(money(100).getAmount()));
    }

    @Test
    public void testSemanticEditUpdatesOnlySelectedRepeatedLeg()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.SPIN_OFF) //
                        .date(DATE) //
                        .effectiveDate(LocalDate.of(2020, 9, 28)) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.target, Values.Share.factorize(5)) //
                        .securityIn("target-2", "main", fixture.portfolio, fixture.secondTarget,
                                        Values.Share.factorize(2)) //
                        .buildAndAdd().getEntry();

        LedgerCorporateActionEditSupport.mutatePosting(fixture.client, entry, LedgerLegRole.TARGET_SECURITY_LEG,
                        "target-2", "main", posting -> posting.setShares(Values.Share.factorize(8)));

        var liveEntry = fixture.client.getLedger().getEntries().get(0);
        assertThat(posting(liveEntry, LedgerLegRole.TARGET_SECURITY_LEG, "target-1").getShares(),
                        is(Values.Share.factorize(5)));
        assertThat(posting(liveEntry, LedgerLegRole.TARGET_SECURITY_LEG, "target-2").getShares(),
                        is(Values.Share.factorize(8)));
    }

    @Test
    public void testSecurityWithoutPortfolioFailsValidation()
    {
        var fixture = fixture();
        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                                        .kind(CorporateActionKind.CASH_DISTRIBUTION) //
                                        .date(DATE) //
                                        .securityContext("context-1", "main", null, fixture.source) //
                                        .cash("cash-1", fixture.account, money(15)) //
                                        .buildDetached());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.STRUCTURAL_VALIDATION_FAILED));
    }

    @Test
    public void testFluentCreatedEntryCanBeDeletedThroughNativeMutationContext()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.SPIN_OFF) //
                        .date(DATE) //
                        .effectiveDate(LocalDate.of(2020, 9, 28)) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.target, Values.Share.factorize(5)) //
                        .cash("cash-1", fixture.account, money(3)) //
                        .buildAndAdd().getEntry();

        assertThat(fixture.client.getLedger().getEntries().size(), is(1));

        new LedgerMutationContext(fixture.client).removeEntry(entry);

        assertThat(fixture.client.getLedger().getEntries().size(), is(0));
        assertTrue(fixture.account.getTransactions().isEmpty());
        assertTrue(fixture.portfolio.getTransactions().isEmpty());
    }

    private static LedgerPosting posting(name.abuchen.portfolio.model.ledger.LedgerEntry entry, LedgerLegRole role,
                    String localKey)
    {
        return LedgerCorporateActionEditSupport.postingBySemanticKey(entry, role, localKey, null);
    }

    private static Money money(long amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private static Fixture fixture()
    {
        var client = new Client();
        var account = new Account();
        var portfolio = new Portfolio();
        var source = new Security("Source AG", CurrencyUnit.EUR);
        var target = new Security("Target AG", CurrencyUnit.EUR);
        var secondTarget = new Security("Second Target AG", CurrencyUnit.EUR);

        account.setName("Cash");
        account.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setName("Portfolio");

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(source);
        client.addSecurity(target);
        client.addSecurity(secondTarget);

        return new Fixture(client, account, portfolio, source, target, secondTarget);
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Security source, Security target,
                    Security secondTarget)
    {
    }
}
