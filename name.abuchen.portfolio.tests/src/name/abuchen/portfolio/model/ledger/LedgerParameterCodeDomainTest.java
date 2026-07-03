package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterCodeDomain;

/**
 * Tests ledger configuration and validation metadata.
 * These tests make sure entry definitions, posting rules, and parameter domains stay stable for Ledger-V6 transactions.
 */
@SuppressWarnings("nls")
public class LedgerParameterCodeDomainTest
{
    private static final Map<LedgerParameterCodeDomain, List<String>> EXPECTED_CODES = Map.ofEntries(
                    Map.entry(LedgerParameterCodeDomain.CORPORATE_ACTION_LEG,
                                    List.of("SOURCE_SECURITY", "TARGET_SECURITY", "DISTRIBUTED_SECURITY",
                                                    "RIGHT_SECURITY", "CASH_COMPENSATION", "CASH_IN_LIEU", "FEE",
                                                    "TAX", "ACCRUED_INTEREST", "PRINCIPAL", "REDEMPTION",
                                                    "CONVERSION_SOURCE", "CONVERSION_TARGET", "OTHER")),
                    Map.entry(LedgerParameterCodeDomain.CORPORATE_ACTION_KIND,
                                    List.of("STOCK_DIVIDEND", "SPIN_OFF", "BONUS_ISSUE",
                                                    "RIGHTS_DISTRIBUTION", "COUPON_PAYMENT", "PIK_INTEREST",
                                                    "DEFAULTED_INTEREST", "MATURITY", "PARTIAL_REDEMPTION",
                                                    "CALL", "PUT", "CONVERSION", "EXCHANGE", "RESTRUCTURING",
                                                    "DEFAULT")),
                    Map.entry(LedgerParameterCodeDomain.CORPORATE_ACTION_SUBTYPE,
                                    List.of("STANDARD", "OPTIONAL", "MANDATORY", "CASH_AND_STOCK", "OTHER")),
                    Map.entry(LedgerParameterCodeDomain.EVENT_STAGE,
                                    List.of("ANNOUNCED", "RECORD", "EX_DATE", "PAYMENT", "ISSUED", "EXERCISED",
                                                    "SOLD", "EXPIRED", "SETTLED", "OTHER")),
                    Map.entry(LedgerParameterCodeDomain.CASH_COMPENSATION_KIND,
                                    List.of("CASH_IN_LIEU", "FRACTIONAL_SHARE_COMPENSATION",
                                                    "ROUNDING_COMPENSATION", "OTHER")),
                    Map.entry(LedgerParameterCodeDomain.FRACTION_TREATMENT,
                                    List.of("NONE", "CASH_IN_LIEU", "ROUND_DOWN", "ROUND_UP", "DROP", "OTHER")),
                    Map.entry(LedgerParameterCodeDomain.ROUNDING_MODE,
                                    List.of("NONE", "FLOOR", "CEILING", "HALF_UP", "HALF_EVEN", "OTHER")),
                    Map.entry(LedgerParameterCodeDomain.COST_ALLOCATION_METHOD,
                                    List.of("NONE", "FMV_RATIO", "MANUAL_PERCENTAGE", "ZERO_COST_TARGET",
                                                    "CARRY_OVER", "OTHER")),
                    Map.entry(LedgerParameterCodeDomain.QUOTATION_STYLE,
                                    List.of("UNIT", "PERCENT", "NOMINAL", "OTHER")),
                    Map.entry(LedgerParameterCodeDomain.FEE_REASON,
                                    List.of("BROKER_FEE", "EXCHANGE_FEE", "CORPORATE_ACTION_FEE", "STAMP_DUTY",
                                                    "OTHER")),
                    Map.entry(LedgerParameterCodeDomain.TAX_REASON,
                                    List.of("WITHHOLDING_TAX", "CAPITAL_GAINS_TAX", "TRANSACTION_TAX",
                                                    "STAMP_DUTY", "RECLAIMABLE_TAX", "OTHER")));

    /**
     * Checks the ledger rule scenario: allowed codes remain persisted string contract.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testAllowedCodesRemainPersistedStringContract()
    {
        assertThat(EXPECTED_CODES.keySet(), is(EnumSet.allOf(LedgerParameterCodeDomain.class)));

        for (var domain : LedgerParameterCodeDomain.values())
            assertThat(domain.getAllowedCodes(), is(EXPECTED_CODES.get(domain)));
    }

    /**
     * Checks the ledger rule scenario: allows remains compatible.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testAllowsRemainsCompatible()
    {
        for (var domain : LedgerParameterCodeDomain.values())
        {
            assertFalse(domain.getAllowedCodes().isEmpty());
            assertThat(domain.getAllowedCodes().stream().distinct().count(), is((long) domain.getAllowedCodes().size()));
            assertTrue(domain.getAllowedCodes().stream().allMatch(code -> code.equals(code.toUpperCase(Locale.ROOT))));

            for (var code : domain.getAllowedCodes())
                assertTrue(domain.allows(code));

            assertFalse(domain.allows("UNKNOWN_CODE"));
        }
    }
}
