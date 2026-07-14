package name.abuchen.portfolio.model.ledger.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.ledger.LedgerPostingDirection;
import name.abuchen.portfolio.model.ledger.LedgerPostingSemanticRole;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerRoleRules.OwnerKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerRoleRules.PrimaryRule;

/**
 * Tests the role-derivation table: every entry type must define its semantic
 * shape, and the direction-required rule must fire exactly for entry types
 * that use the same posting type for more than one primary posting.
 */
@SuppressWarnings("nls")
public class LedgerRoleRulesTest
{
    @Test
    public void testEveryEntryTypeDefinesPrimaryRules()
    {
        for (var type : LedgerEntryType.values())
            assertFalse("no primary rules for " + type, LedgerRoleRules.primaryRules(type).isEmpty());
    }

    @Test
    public void testAccountOnlyEntryTypesUseNeutralCashShape()
    {
        for (var type : List.of(LedgerEntryType.DEPOSIT, LedgerEntryType.REMOVAL, LedgerEntryType.INTEREST,
                        LedgerEntryType.INTEREST_CHARGE, LedgerEntryType.DIVIDENDS))
            assertThat(LedgerRoleRules.primaryRules(type),
                            is(List.of(new PrimaryRule(LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH,
                                            LedgerPostingDirection.NEUTRAL, LedgerProjectionRole.ACCOUNT,
                                            OwnerKind.ACCOUNT))));
    }

    @Test
    public void testFeeAndTaxEntryTypesUseMatchingPostingTypes()
    {
        for (var type : List.of(LedgerEntryType.FEES, LedgerEntryType.FEES_REFUND))
            assertThat(LedgerRoleRules.primaryRules(type),
                            is(List.of(new PrimaryRule(LedgerPostingType.FEE, LedgerPostingSemanticRole.FEE,
                                            LedgerPostingDirection.NEUTRAL, LedgerProjectionRole.ACCOUNT,
                                            OwnerKind.ACCOUNT))));

        for (var type : List.of(LedgerEntryType.TAXES, LedgerEntryType.TAX_REFUND))
            assertThat(LedgerRoleRules.primaryRules(type),
                            is(List.of(new PrimaryRule(LedgerPostingType.TAX, LedgerPostingSemanticRole.TAX,
                                            LedgerPostingDirection.NEUTRAL, LedgerProjectionRole.ACCOUNT,
                                            OwnerKind.ACCOUNT))));
    }

    @Test
    public void testBuyMovesCashOutAndSecurityIn()
    {
        assertThat(LedgerRoleRules.primaryRules(LedgerEntryType.BUY), is(List.of(
                        new PrimaryRule(LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH,
                                        LedgerPostingDirection.OUTBOUND, LedgerProjectionRole.ACCOUNT,
                                        OwnerKind.ACCOUNT),
                        new PrimaryRule(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingDirection.INBOUND, LedgerProjectionRole.PORTFOLIO,
                                        OwnerKind.PORTFOLIO))));
    }

    @Test
    public void testSellMovesCashInAndSecurityOut()
    {
        assertThat(LedgerRoleRules.primaryRules(LedgerEntryType.SELL), is(List.of(
                        new PrimaryRule(LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH,
                                        LedgerPostingDirection.INBOUND, LedgerProjectionRole.ACCOUNT,
                                        OwnerKind.ACCOUNT),
                        new PrimaryRule(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingDirection.OUTBOUND, LedgerProjectionRole.PORTFOLIO,
                                        OwnerKind.PORTFOLIO))));
    }

    @Test
    public void testDeliveriesEncodeDirectionInTheEntryType()
    {
        assertThat(LedgerRoleRules.primaryRules(LedgerEntryType.DELIVERY_INBOUND), is(List.of(
                        new PrimaryRule(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingDirection.INBOUND, LedgerProjectionRole.DELIVERY_INBOUND,
                                        OwnerKind.PORTFOLIO))));
        assertThat(LedgerRoleRules.primaryRules(LedgerEntryType.DELIVERY_OUTBOUND), is(List.of(
                        new PrimaryRule(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingDirection.OUTBOUND, LedgerProjectionRole.DELIVERY_OUTBOUND,
                                        OwnerKind.PORTFOLIO))));
    }

    @Test
    public void testTransfersMapOutboundToSourceAndInboundToTarget()
    {
        assertThat(LedgerRoleRules.primaryRules(LedgerEntryType.CASH_TRANSFER), is(List.of(
                        new PrimaryRule(LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH,
                                        LedgerPostingDirection.OUTBOUND, LedgerProjectionRole.SOURCE_ACCOUNT,
                                        OwnerKind.ACCOUNT),
                        new PrimaryRule(LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH,
                                        LedgerPostingDirection.INBOUND, LedgerProjectionRole.TARGET_ACCOUNT,
                                        OwnerKind.ACCOUNT))));
        assertThat(LedgerRoleRules.primaryRules(LedgerEntryType.SECURITY_TRANSFER), is(List.of(
                        new PrimaryRule(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingDirection.OUTBOUND, LedgerProjectionRole.SOURCE_PORTFOLIO,
                                        OwnerKind.PORTFOLIO),
                        new PrimaryRule(LedgerPostingType.SECURITY, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingDirection.INBOUND, LedgerProjectionRole.TARGET_PORTFOLIO,
                                        OwnerKind.PORTFOLIO))));
    }

    @Test
    public void testDirectionIsRequiredExactlyForDuplicatedPostingTypes()
    {
        assertTrue(LedgerRoleRules.isDirectionRequired(LedgerEntryType.CASH_TRANSFER, LedgerPostingType.CASH));
        assertTrue(LedgerRoleRules.isDirectionRequired(LedgerEntryType.SECURITY_TRANSFER,
                        LedgerPostingType.SECURITY));

        for (var type : LedgerEntryType.values())
        {
            if (type == LedgerEntryType.CASH_TRANSFER || type == LedgerEntryType.SECURITY_TRANSFER)
                continue;

            for (var postingType : LedgerPostingType.values())
                assertFalse("direction must be derivable for " + type + "/" + postingType,
                                LedgerRoleRules.isDirectionRequired(type, postingType));
        }
    }

    @Test
    public void testUniquePrimaryRuleIsDerivableWithoutDirection()
    {
        var rule = LedgerRoleRules.primaryRule(LedgerEntryType.BUY, LedgerPostingType.CASH);

        assertTrue(rule.isPresent());
        assertThat(rule.get().direction(), is(LedgerPostingDirection.OUTBOUND));
        assertThat(rule.get().projectionRole(), is(LedgerProjectionRole.ACCOUNT));
    }

    @Test
    public void testAmbiguousPrimaryRuleIsNotDerivableWithoutDirection()
    {
        assertTrue(LedgerRoleRules.primaryRule(LedgerEntryType.CASH_TRANSFER, LedgerPostingType.CASH).isEmpty());
    }

    @Test
    public void testAmbiguousPrimaryRuleIsDerivableWithDirection()
    {
        var source = LedgerRoleRules.primaryRule(LedgerEntryType.CASH_TRANSFER, LedgerPostingType.CASH,
                        LedgerPostingDirection.OUTBOUND);
        var target = LedgerRoleRules.primaryRule(LedgerEntryType.CASH_TRANSFER, LedgerPostingType.CASH,
                        LedgerPostingDirection.INBOUND);

        assertThat(source.orElseThrow().projectionRole(), is(LedgerProjectionRole.SOURCE_ACCOUNT));
        assertThat(target.orElseThrow().projectionRole(), is(LedgerProjectionRole.TARGET_ACCOUNT));
    }

    @Test
    public void testUnitRulesCoverFeeTaxAndGrossValue()
    {
        assertThat(LedgerRoleRules.unitRule(LedgerPostingType.FEE).orElseThrow().unitRole(),
                        is(LedgerPostingUnitRole.FEE));
        assertThat(LedgerRoleRules.unitRule(LedgerPostingType.TAX).orElseThrow().unitRole(),
                        is(LedgerPostingUnitRole.TAX));
        assertThat(LedgerRoleRules.unitRule(LedgerPostingType.GROSS_VALUE).orElseThrow().unitRole(),
                        is(LedgerPostingUnitRole.GROSS_VALUE));
        assertTrue(LedgerRoleRules.unitRule(LedgerPostingType.CASH).isEmpty());
        assertTrue(LedgerRoleRules.unitRule(LedgerPostingType.SECURITY).isEmpty());
    }
}
