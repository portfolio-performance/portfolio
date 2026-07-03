package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.Test;

import name.abuchen.portfolio.model.ledger.configuration.CashCompensationKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionSubtype;
import name.abuchen.portfolio.model.ledger.configuration.CostAllocationMethod;
import name.abuchen.portfolio.model.ledger.configuration.EventStage;
import name.abuchen.portfolio.model.ledger.configuration.FeeReason;
import name.abuchen.portfolio.model.ledger.configuration.FractionTreatment;
import name.abuchen.portfolio.model.ledger.configuration.LedgerCode;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterCodeDomain;
import name.abuchen.portfolio.model.ledger.configuration.QuotationStyle;
import name.abuchen.portfolio.model.ledger.configuration.RoundingModeCode;
import name.abuchen.portfolio.model.ledger.configuration.TaxReason;

/**
 * Tests ledger configuration and validation metadata.
 * These tests make sure entry definitions, posting rules, and parameter domains stay stable for Ledger-V6 transactions.
 */
@SuppressWarnings("nls")
public class LedgerCodeTest
{
    /**
     * Checks the ledger rule scenario: every code belongs to one domain and is uppercase.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testEveryCodeBelongsToOneDomainAndIsUppercase()
    {
        for (var code : allCodes())
        {
            assertTrue(EnumSet.allOf(LedgerParameterCodeDomain.class).contains(code.getDomain()));
            assertFalse(code.getCode().isBlank());
            assertThat(code.getCode(), is(code.getCode().toUpperCase(Locale.ROOT)));
        }
    }

    /**
     * Checks the ledger rule scenario: no duplicate codes within domain.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testNoDuplicateCodesWithinDomain()
    {
        var seen = new EnumMap<LedgerParameterCodeDomain, HashSet<String>>(LedgerParameterCodeDomain.class);

        for (var domain : LedgerParameterCodeDomain.values())
            seen.put(domain, new HashSet<>());

        for (var code : allCodes())
            assertTrue(code.getDomain() + ":" + code.getCode(), seen.get(code.getDomain()).add(code.getCode()));
    }

    /**
     * Checks the ledger rule scenario: domain allowed codes match domain enums.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testDomainAllowedCodesMatchDomainEnums()
    {
        for (var domain : LedgerParameterCodeDomain.values())
        {
            var codes = allCodes().stream().filter(code -> code.getDomain() == domain).toList();

            assertFalse(codes.isEmpty());
            assertThat(codes.stream().map(LedgerCode::getCode).toList(), is(domain.getAllowedCodes()));
        }
    }

    /**
     * Checks the ledger rule scenario: corporate action kinds classify the generic
     * native corporate action entry family.
     */
    @Test
    public void testCorporateActionKindsClassifyGenericNativeEntryType()
    {
        assertTrue(LedgerEntryType.CORPORATE_ACTION.isLedgerNativeTargeted());
        assertThat(List.of(CorporateActionKind.values()), is(List.of(
                        CorporateActionKind.STOCK_DIVIDEND,
                        CorporateActionKind.SPIN_OFF,
                        CorporateActionKind.BONUS_ISSUE,
                        CorporateActionKind.RIGHTS_DISTRIBUTION,
                        CorporateActionKind.COUPON_PAYMENT,
                        CorporateActionKind.PIK_INTEREST,
                        CorporateActionKind.DEFAULTED_INTEREST,
                        CorporateActionKind.MATURITY,
                        CorporateActionKind.PARTIAL_REDEMPTION,
                        CorporateActionKind.CALL,
                        CorporateActionKind.PUT,
                        CorporateActionKind.CONVERSION,
                        CorporateActionKind.EXCHANGE,
                        CorporateActionKind.RESTRUCTURING,
                        CorporateActionKind.DEFAULT)));
        assertThat(CorporateActionKind.fromCode("SPIN_OFF").orElseThrow(), is(CorporateActionKind.SPIN_OFF));
        assertThat(CorporateActionKind.fromCode("MATURITY").orElseThrow(), is(CorporateActionKind.MATURITY));
        assertTrue(CorporateActionKind.fromCode("OTHER").isEmpty());
    }

    private List<LedgerCode> allCodes()
    {
        return Stream.<LedgerCode[]>of(
                        CorporateActionLeg.values(),
                        CorporateActionKind.values(),
                        CorporateActionSubtype.values(),
                        EventStage.values(),
                        CashCompensationKind.values(),
                        FractionTreatment.values(),
                        RoundingModeCode.values(),
                        CostAllocationMethod.values(),
                        QuotationStyle.values(),
                        FeeReason.values(),
                        TaxReason.values()).flatMap(Arrays::stream).toList();
    }
}
