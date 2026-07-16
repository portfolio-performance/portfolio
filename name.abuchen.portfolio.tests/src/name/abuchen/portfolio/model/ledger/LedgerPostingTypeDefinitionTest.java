package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.EnumSet;

import org.junit.Test;

import name.abuchen.portfolio.model.ledger.configuration.FeeReason;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinitionRegistry;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEventParameterDefinition;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingTypeDefinitionRegistry;
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * Tests ledger configuration and validation metadata.
 * These tests make sure entry definitions, posting rules, and parameter domains stay stable for Ledger-V6 transactions.
 */
@SuppressWarnings("nls")
public class LedgerPostingTypeDefinitionTest
{
    /**
     * Checks the ledger rule scenario: every posting type has definition.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testEveryPostingTypeHasDefinition()
    {
        for (var postingType : LedgerPostingType.values())
        {
            var definition = LedgerPostingTypeDefinitionRegistry.lookup(postingType).orElseThrow();

            assertThat(definition.getPostingType(), is(postingType));
            assertFalse(definition.getComponentParameterTypes().isEmpty());
        }

        assertThat(LedgerPostingTypeDefinitionRegistry.getDefinitions().size(), is(LedgerPostingType.values().length));
    }

    /**
     * Checks the ledger rule scenario: posting type definitions are consistent static configuration.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testPostingTypeDefinitionsAreConsistentStaticConfiguration()
    {
        for (var definition : LedgerPostingTypeDefinitionRegistry.getDefinitions())
        {
            assertTrue(LedgerPostingTypeDefinitionRegistry.hasDefinition(definition.getPostingType()));

            for (var parameterType : definition.getComponentParameterTypes())
            {
                assertTrue(definition.supportsParameterType(parameterType));
                assertTrue(EnumSet.allOf(LedgerParameterType.class).contains(parameterType));
            }
        }
    }

    /**
     * Checks the ledger rule scenario: component fact mapping sanity.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testComponentFactMappingSanity()
    {
        assertSupports(LedgerPostingType.CASH, LedgerParameterType.SOURCE_ACCOUNT, LedgerParameterType.TARGET_ACCOUNT,
                        LedgerParameterType.CASH_ACCOUNT, LedgerParameterType.PAYMENT_DATE);

        assertSupports(LedgerPostingType.SECURITY, LedgerParameterType.SOURCE_SECURITY,
                        LedgerParameterType.TARGET_SECURITY, LedgerParameterType.RATIO_NUMERATOR,
                        LedgerParameterType.FRACTION_TREATMENT, LedgerParameterType.COST_ALLOCATION_METHOD,
                        LedgerParameterType.REFERENCE_PRICE, LedgerParameterType.CORPORATE_ACTION_LEG);

        assertSupports(LedgerPostingType.CASH_COMPENSATION, LedgerParameterType.CASH_COMPENSATION_KIND,
                        LedgerParameterType.CASH_IN_LIEU_AMOUNT, LedgerParameterType.CASH_IN_LIEU_APPLIED,
                        LedgerParameterType.FRACTION_QUANTITY, LedgerParameterType.CORPORATE_ACTION_LEG);

        assertSupports(LedgerPostingType.FEE, LedgerParameterType.FEE_REASON);
        assertSupports(LedgerPostingType.TAX, LedgerParameterType.TAX_REASON, LedgerParameterType.WITHHOLDING_TAX,
                        LedgerParameterType.RECLAIMABLE_TAX);
        assertSupports(LedgerPostingType.GROSS_VALUE, LedgerParameterType.REFERENCE_PRICE,
                        LedgerParameterType.VALUATION_PRICE, LedgerParameterType.FAIR_MARKET_VALUE);
        assertSupports(LedgerPostingType.FOREX, LedgerParameterType.REFERENCE_PRICE,
                        LedgerParameterType.VALUATION_PRICE);
        assertSupports(LedgerPostingType.RIGHT, LedgerParameterType.RIGHT_SECURITY,
                        LedgerParameterType.SUBSCRIPTION_PRICE, LedgerParameterType.ELECTION_DEADLINE);
        assertSupports(LedgerPostingType.BOND, LedgerParameterType.NOMINAL_VALUE,
                        LedgerParameterType.QUOTATION_STYLE, LedgerParameterType.CONVERSION_RATIO,
                        LedgerParameterType.REDEMPTION_PRICE_PERCENT);
        assertSupports(LedgerPostingType.ACCRUED_INTEREST, LedgerParameterType.ACCRUED_INTEREST_AMOUNT,
                        LedgerParameterType.COUPON_RATE, LedgerParameterType.INTEREST_PERIOD_START,
                        LedgerParameterType.INTEREST_PERIOD_END);
        assertSupports(LedgerPostingType.PRINCIPAL_REDEMPTION, LedgerParameterType.NOMINAL_VALUE,
                        LedgerParameterType.REDEMPTION_PRICE_PERCENT,
                        LedgerParameterType.PARTIAL_REDEMPTION_FACTOR);
    }

    /**
     * Checks the ledger rule scenario: event level parameters are classified separately.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testEventLevelParametersAreClassifiedSeparately()
    {
        assertTrue(LedgerEventParameterDefinition.supportsParameterType(LedgerParameterType.CORPORATE_ACTION_KIND));
        assertTrue(LedgerEventParameterDefinition.supportsParameterType(LedgerParameterType.CORPORATE_ACTION_SUBTYPE));
        assertTrue(LedgerEventParameterDefinition.supportsParameterType(LedgerParameterType.EVENT_REFERENCE));
        assertTrue(LedgerEventParameterDefinition.supportsParameterType(LedgerParameterType.EVENT_STAGE));
        assertTrue(LedgerEventParameterDefinition.supportsParameterType(LedgerParameterType.EX_DATE));
        assertTrue(LedgerEventParameterDefinition.supportsParameterType(LedgerParameterType.ELECTION_DEADLINE));
        assertFalse(LedgerEventParameterDefinition.supportsParameterType(LedgerParameterType.FEE_REASON));
    }

    /**
     * Checks the ledger rule scenario: native entry definitions reference defined posting fact vocabulary.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testNativeEntryDefinitionsReferenceDefinedPostingFactVocabulary()
    {
        for (var entryDefinition : LedgerEntryDefinitionRegistry.getDefinitions())
        {
            for (var postingType : entryDefinition.getPostingTypes())
                assertTrue(LedgerPostingTypeDefinitionRegistry.hasDefinition(postingType));

            for (var parameterType : entryDefinition.getEntryParameterTypes())
                assertTrue(parameterType.name(), isEventOrPostingFact(entryDefinition.getPostingTypes(), parameterType));

            for (var parameterType : entryDefinition.getPostingParameterTypes())
                assertTrue(parameterType.name(), isEventOrPostingFact(entryDefinition.getPostingTypes(), parameterType));
        }
    }

    /**
     * Checks the ledger rule scenario: definition layer does not enforce posting fact completeness.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testDefinitionLayerDoesNotEnforcePostingFactCompleteness()
    {
        var ledger = new Ledger();
        var entry = new LedgerEntry("entry-1");
        var posting = new LedgerPosting("posting-1");

        entry.setType(LedgerEntryType.CORPORATE_ACTION);
        entry.setDateTime(LocalDateTime.of(2026, 1, 2, 0, 0));
        posting.setType(LedgerPostingType.CASH);
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.CASH);
        posting.setDirection(LedgerPostingDirection.NEUTRAL);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.FEE_REASON,
                        FeeReason.BROKER_FEE.getCode()));
        entry.addPosting(posting);
        ledger.addEntry(entry);

        assertTrue(LedgerStructuralValidator.validate(ledger).isOK());
        assertFalse(LedgerPostingTypeDefinitionRegistry.lookup(LedgerPostingType.CASH).orElseThrow()
                        .supportsParameterType(LedgerParameterType.FEE_REASON));
    }

    private void assertSupports(LedgerPostingType postingType, LedgerParameterType... parameterTypes)
    {
        var definition = LedgerPostingTypeDefinitionRegistry.lookup(postingType).orElseThrow();

        for (var parameterType : parameterTypes)
            assertTrue(parameterType.name(), definition.supportsParameterType(parameterType));
    }

    private boolean isEventOrPostingFact(Iterable<LedgerPostingType> postingTypes, LedgerParameterType parameterType)
    {
        if (LedgerEventParameterDefinition.supportsParameterType(parameterType))
            return true;

        for (var postingType : postingTypes)
        {
            var definition = LedgerPostingTypeDefinitionRegistry.lookup(postingType).orElseThrow();

            if (definition.supportsParameterType(parameterType))
                return true;
        }

        return false;
    }
}
