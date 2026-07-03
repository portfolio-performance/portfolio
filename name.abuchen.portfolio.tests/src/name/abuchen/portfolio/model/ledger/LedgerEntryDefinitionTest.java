package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;

import org.junit.Test;

import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerDownstreamResult;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinitionRegistry;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegCardinality;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegDefinition;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerNativeEntryShape;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterCodeDomain;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPerformanceTreatment;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingTypeDefinitionRegistry;
import name.abuchen.portfolio.model.ledger.configuration.LedgerReportingClass;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerParameterRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerPostingRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerProjectionRule;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerRequirement;
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * Tests ledger configuration and validation metadata.
 * These tests make sure entry definitions, posting rules, and parameter domains stay stable for Ledger-V6 transactions.
 */
@SuppressWarnings("nls")
public class LedgerEntryDefinitionTest
{
    /**
     * Checks the ledger rule scenario: ledger native entry types have definitions.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testLedgerNativeEntryTypesHaveDefinitions()
    {
        for (var type : LedgerEntryType.values())
        {
            if (type.isLedgerNativeTargeted())
            {
                var definition = LedgerEntryDefinitionRegistry.lookup(type).orElseThrow();

                assertThat(definition.getEntryType(), is(type));
            }
            else
            {
                assertFalse(type.name(), LedgerEntryDefinitionRegistry.hasDefinition(type));
            }
        }
    }

    /**
     * Checks the ledger rule scenario: definitions are consistent static configuration.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testDefinitionsAreConsistentStaticConfiguration()
    {
        for (var definition : LedgerEntryDefinitionRegistry.getDefinitions())
        {
            assertTrue(definition.getEntryType().isLedgerNativeTargeted());
            assertTrue(definition.getNativeShape() != LedgerNativeEntryShape.UNDEFINED);
            assertFalse(definition.getPostingTypes().isEmpty());
            assertFalse(definition.getPostingRules().isEmpty());
            assertFalse(definition.getEntryParameterTypes().isEmpty());
            assertFalse(definition.getRequiredEntryParameterRules().isEmpty());
            assertFalse(definition.getEntryParameterRules().isEmpty());
            assertFalse(definition.getPostingParameterTypes().isEmpty());
            assertFalse(definition.getPostingParameterRules().isEmpty());
            assertFalse(definition.getLegDefinitions().isEmpty());
            assertTrue(definition.getReportingClass() != LedgerReportingClass.UNDEFINED);
            assertTrue(definition.getPerformanceTreatment() != LedgerPerformanceTreatment.UNDEFINED);
            assertThat(definition.getDownstreamResultsNotPersisted(), is(EnumSet.allOf(LedgerDownstreamResult.class)));
        }
    }

    /**
     * Checks the ledger rule scenario: definition registries expose read only schemas.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testDefinitionRegistriesExposeReadOnlySchemas()
    {
        for (var entryType : LedgerEntryType.values())
        {
            if (!entryType.isLedgerNativeTargeted())
                continue;

            var definition = LedgerEntryDefinitionRegistry.lookup(entryType).orElseThrow();

            assertThat(definition.getEntryType(), is(entryType));
            assertFalse(definition.getPostingRules().isEmpty());
            assertFalse(definition.getEntryParameterRules().isEmpty());
            assertFalse(definition.getPostingParameterRules().isEmpty());
            assertTrue(definition.getReportingClass() != LedgerReportingClass.UNDEFINED);
            assertTrue(definition.getPerformanceTreatment() != LedgerPerformanceTreatment.UNDEFINED);
            assertThat(definition.getDownstreamResultsNotPersisted(), is(EnumSet.allOf(LedgerDownstreamResult.class)));
        }

        for (var postingType : LedgerPostingType.values())
        {
            var definition = LedgerPostingTypeDefinitionRegistry.lookup(postingType).orElseThrow();

            assertThat(definition.getPostingType(), is(postingType));
            assertFalse(postingType.name(), definition.getComponentParameterTypes().isEmpty());
        }
    }

    /**
     * Checks the ledger rule scenario: rule keys are unique within definition categories.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testRuleKeysAreUniqueWithinDefinitionCategories()
    {
        for (var definition : LedgerEntryDefinitionRegistry.getDefinitions())
        {
            assertUniquePostingRules(definition);
            assertUniqueParameterRules(definition, definition.getEntryParameterRules(), "entry parameter rule");
            assertUniqueParameterRules(definition, definition.getPostingParameterRules(), "posting parameter rule");
            assertUniqueProjectionRules(definition);
            assertUniquePostingGroupRules(definition);
            assertUniqueAlternativeGroupRules(definition);
            assertUniqueLegDefinitions(definition);
        }
    }

    /**
     * Checks the ledger rule scenario: alternative requirement groups are real alternatives.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testAlternativeRequirementGroupsAreRealAlternatives()
    {
        for (var definition : LedgerEntryDefinitionRegistry.getDefinitions())
        {
            for (var group : definition.getAlternativeRequirementGroups())
            {
                var numberOfAlternatives = group.getPostingTypes().size() + group.getParameterTypes().size();

                assertTrue(definition.getEntryType() + ":" + group.getName(), numberOfAlternatives >= 2);
                assertTrue(definition.getEntryType() + ":" + group.getName(),
                                group.getPostingTypes().isEmpty() != group.getParameterTypes().isEmpty());
            }
        }
    }

    /**
     * Checks the ledger rule scenario: alternative groups do not duplicate hard required members.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testAlternativeGroupsDoNotDuplicateHardRequiredMembers()
    {
        for (var definition : LedgerEntryDefinitionRegistry.getDefinitions())
        {
            var requiredPostingTypes = EnumSet.noneOf(LedgerPostingType.class);
            var requiredParameterTypes = EnumSet.noneOf(LedgerParameterType.class);

            for (var rule : definition.getRequiredPostingRules())
                requiredPostingTypes.add(rule.getPostingType());

            for (var rule : definition.getRequiredEntryParameterRules())
                requiredParameterTypes.add(rule.getParameterType());

            for (var rule : definition.getRequiredPostingParameterRules())
                requiredParameterTypes.add(rule.getParameterType());

            for (var rule : definition.getPostingRules())
                requiredParameterTypes.addAll(rule.getRequiredParameterTypes());

            for (var group : definition.getAlternativeRequirementGroups())
            {
                for (var postingType : group.getPostingTypes())
                    assertFalse(definition.getEntryType() + ":" + group.getName() + ":" + postingType,
                                    requiredPostingTypes.contains(postingType));

                for (var parameterType : group.getParameterTypes())
                    assertFalse(definition.getEntryType() + ":" + group.getName() + ":" + parameterType,
                                    requiredParameterTypes.contains(parameterType));
            }
        }
    }

    /**
     * Checks the ledger rule scenario: spin off definition describes native data model.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testSpinOffDefinitionDescribesNativeDataModel()
    {
        var definition = LedgerEntryDefinitionRegistry
                        .lookup(LedgerEntryType.CORPORATE_ACTION, CorporateActionKind.SPIN_OFF).orElseThrow();

        assertThat(definition.getNativeShape(), is(LedgerNativeEntryShape.DUAL_INSTRUMENT_PLUS_ACCOUNT));
        assertTrue(definition.getPostingTypes().contains(LedgerPostingType.SECURITY));
        assertTrue(definition.getPostingTypes().contains(LedgerPostingType.CASH_COMPENSATION));
        assertTrue(definition.getPostingTypes().contains(LedgerPostingType.FEE));
        assertTrue(definition.getPostingTypes().contains(LedgerPostingType.TAX));
        assertTrue(definition.getPostingTypes().contains(LedgerPostingType.FOREX));
        assertTrue(definition.getEntryParameterTypes().contains(LedgerParameterType.CORPORATE_ACTION_KIND));
        assertTrue(definition.getEntryParameterTypes().contains(LedgerParameterType.EX_DATE));
        assertTrue(definition.getPostingParameterTypes().contains(LedgerParameterType.CORPORATE_ACTION_LEG));
        assertTrue(definition.getPostingParameterTypes().contains(LedgerParameterType.SOURCE_SECURITY));
        assertTrue(definition.getPostingParameterTypes().contains(LedgerParameterType.TARGET_SECURITY));
        assertTrue(definition.getProjectionRoles().contains(LedgerProjectionRole.OLD_SECURITY_LEG));
        assertTrue(definition.getProjectionRoles().contains(LedgerProjectionRole.NEW_SECURITY_LEG));
        assertTrue(definition.getProjectionRoles().contains(LedgerProjectionRole.CASH_COMPENSATION));
        assertOptionalPosting(definition, LedgerPostingType.SECURITY);
        assertOptionalPosting(definition, LedgerPostingType.CASH_COMPENSATION);
        assertOptionalPosting(definition, LedgerPostingType.FEE);
        assertOptionalPosting(definition, LedgerPostingType.TAX);
        assertOptionalPosting(definition, LedgerPostingType.FOREX);
        assertRequiredEntryParameter(definition, LedgerParameterType.CORPORATE_ACTION_KIND);
        assertOptionalEntryParameter(definition, LedgerParameterType.EX_DATE);
        assertOptionalEntryParameter(definition, LedgerParameterType.EFFECTIVE_DATE);
        assertRequiredPostingParameter(definition, LedgerParameterType.CORPORATE_ACTION_LEG);
        assertRequiredPostingParameter(definition, LedgerParameterType.SOURCE_SECURITY);
        assertRequiredPostingParameter(definition, LedgerParameterType.TARGET_SECURITY);
        assertOptionalPostingParameter(definition, LedgerParameterType.CASH_IN_LIEU_AMOUNT);
        assertRepeatableParameter(definition, LedgerParameterType.CORPORATE_ACTION_LEG);
        assertOptionalProjection(definition, LedgerProjectionRole.OLD_SECURITY_LEG, true, false);
        assertOptionalProjection(definition, LedgerProjectionRole.NEW_SECURITY_LEG, true, false);
        assertOptionalProjection(definition, LedgerProjectionRole.CASH_COMPENSATION, true, true);
        assertAlternativeGroup(definition, "SPIN_OFF_DATE", LedgerRequirement.REQUIRED,
                        LedgerParameterType.EX_DATE, LedgerParameterType.EFFECTIVE_DATE);
        assertThat(definition.getReportingClass(), is(LedgerReportingClass.SECURITIES_DISTRIBUTION));
        assertThat(definition.getPerformanceTreatment(), is(LedgerPerformanceTreatment.COST_BASIS_REALLOCATION));
    }

    /**
     * Checks that the spin-off definition names its business legs explicitly.
     * The old and new security sides both use SECURITY postings, so the leg
     * definitions must carry the business meaning that the posting type cannot.
     */
    @Test
    public void testSpinOffDefinitionDescribesFunctionalLegs()
    {
        var definition = LedgerEntryDefinitionRegistry
                        .lookup(LedgerEntryType.CORPORATE_ACTION, CorporateActionKind.SPIN_OFF).orElseThrow();

        var sourceLeg = assertLeg(definition, LedgerLegRole.SOURCE_SECURITY_LEG, LedgerPostingType.SECURITY,
                        LedgerLegCardinality.REPEATABLE);
        assertTrue(sourceLeg.getRequiredParameterTypes().contains(LedgerParameterType.CORPORATE_ACTION_LEG));
        assertTrue(sourceLeg.getRequiredParameterTypes().contains(LedgerParameterType.SOURCE_SECURITY));
        assertTrue(sourceLeg.getRequiredParameterTypes().contains(LedgerParameterType.RATIO_NUMERATOR));
        assertTrue(sourceLeg.getRequiredParameterTypes().contains(LedgerParameterType.RATIO_DENOMINATOR));
        assertTrue(sourceLeg.getOptionalParameterTypes().contains(LedgerParameterType.TARGET_SECURITY));
        assertThat(sourceLeg.getProjectionRole().orElseThrow(), is(LedgerProjectionRole.OLD_SECURITY_LEG));
        assertTrue(sourceLeg.isPrimaryPostingExpected());
        assertFalse(sourceLeg.isPostingGroupExpected());

        var targetLeg = assertLeg(definition, LedgerLegRole.TARGET_SECURITY_LEG, LedgerPostingType.SECURITY,
                        LedgerLegCardinality.REPEATABLE);
        assertTrue(targetLeg.getRequiredParameterTypes().contains(LedgerParameterType.CORPORATE_ACTION_LEG));
        assertTrue(targetLeg.getRequiredParameterTypes().contains(LedgerParameterType.TARGET_SECURITY));
        assertTrue(targetLeg.getRequiredParameterTypes().contains(LedgerParameterType.RATIO_NUMERATOR));
        assertTrue(targetLeg.getRequiredParameterTypes().contains(LedgerParameterType.RATIO_DENOMINATOR));
        assertTrue(targetLeg.getOptionalParameterTypes().contains(LedgerParameterType.SOURCE_SECURITY));
        assertThat(targetLeg.getProjectionRole().orElseThrow(), is(LedgerProjectionRole.NEW_SECURITY_LEG));
        assertTrue(targetLeg.isPrimaryPostingExpected());
        assertFalse(targetLeg.isPostingGroupExpected());

        var cashLeg = assertLeg(definition, LedgerLegRole.CASH_COMPENSATION_LEG,
                        LedgerPostingType.CASH_COMPENSATION, LedgerLegCardinality.REPEATABLE);
        assertThat(cashLeg.getProjectionRole().orElseThrow(), is(LedgerProjectionRole.CASH_COMPENSATION));
        assertTrue(cashLeg.isPrimaryPostingExpected());
        assertTrue(cashLeg.isPostingGroupExpected());
        assertTrue(cashLeg.getGroupNames().contains("CASH_COMPENSATION_GROUP"));

        var feeLeg = assertLeg(definition, LedgerLegRole.FEE_LEG, LedgerPostingType.FEE,
                        LedgerLegCardinality.REPEATABLE);
        assertTrue(feeLeg.getProjectionRole().isEmpty());
        assertTrue(feeLeg.getGroupNames().contains("CASH_COMPENSATION_GROUP"));

        var taxLeg = assertLeg(definition, LedgerLegRole.TAX_LEG, LedgerPostingType.TAX,
                        LedgerLegCardinality.REPEATABLE);
        assertTrue(taxLeg.getProjectionRole().isEmpty());
        assertTrue(taxLeg.getGroupNames().contains("CASH_COMPENSATION_GROUP"));

        var forexLeg = assertLeg(definition, LedgerLegRole.FOREX_CONTEXT_LEG, LedgerPostingType.FOREX,
                        LedgerLegCardinality.OPTIONAL);
        assertTrue(forexLeg.getProjectionRole().isEmpty());
        assertTrue(forexLeg.getGroupNames().isEmpty());
    }

    /**
     * Checks the ledger rule scenario: corporate action kinds can be registered as stable
     * identities before all Registry.md profile dimensions are modeled. Definitions only
     * include dimensions that current Ledger primitives can express honestly.
     */
    @Test
    public void testCorporateActionKindDefinitionsRegisterRepresentableProfileSubsets()
    {
        for (var kind : new CorporateActionKind[] { CorporateActionKind.STOCK_DIVIDEND,
                        CorporateActionKind.SPIN_OFF, CorporateActionKind.BONUS_ISSUE,
                        CorporateActionKind.RIGHTS_DISTRIBUTION, CorporateActionKind.COUPON_PAYMENT,
                        CorporateActionKind.PIK_INTEREST, CorporateActionKind.MATURITY,
                        CorporateActionKind.PARTIAL_REDEMPTION, CorporateActionKind.CALL,
                        CorporateActionKind.PUT, CorporateActionKind.CONVERSION,
                        CorporateActionKind.EXCHANGE })
            assertTrue(kind.name(), LedgerEntryDefinitionRegistry
                            .lookup(LedgerEntryType.CORPORATE_ACTION, kind).isPresent());

        for (var kind : new CorporateActionKind[] { CorporateActionKind.DEFAULTED_INTEREST,
                        CorporateActionKind.RESTRUCTURING, CorporateActionKind.DEFAULT })
            assertFalse(kind.name(), LedgerEntryDefinitionRegistry
                            .lookup(LedgerEntryType.CORPORATE_ACTION, kind).isPresent());

        var stockDividend = LedgerEntryDefinitionRegistry
                        .lookup(LedgerEntryType.CORPORATE_ACTION, CorporateActionKind.STOCK_DIVIDEND)
                        .orElseThrow();
        assertLeg(stockDividend, LedgerLegRole.TARGET_SECURITY_LEG, LedgerPostingType.SECURITY,
                        LedgerLegCardinality.AT_LEAST_ONE);
        assertLeg(stockDividend, LedgerLegRole.CASH_COMPENSATION_LEG, LedgerPostingType.CASH_COMPENSATION,
                        LedgerLegCardinality.REPEATABLE);
        assertTrue(stockDividend.getProjectionRoles().isEmpty());

        var coupon = LedgerEntryDefinitionRegistry
                        .lookup(LedgerEntryType.CORPORATE_ACTION, CorporateActionKind.COUPON_PAYMENT)
                        .orElseThrow();
        assertLeg(coupon, LedgerLegRole.CASH_LEG, LedgerPostingType.CASH, LedgerLegCardinality.AT_LEAST_ONE);
        assertLeg(coupon, LedgerLegRole.ACCRUED_INTEREST_LEG, LedgerPostingType.ACCRUED_INTEREST,
                        LedgerLegCardinality.OPTIONAL);

        var maturity = LedgerEntryDefinitionRegistry
                        .lookup(LedgerEntryType.CORPORATE_ACTION, CorporateActionKind.MATURITY)
                        .orElseThrow();
        assertLeg(maturity, LedgerLegRole.SOURCE_SECURITY_LEG, LedgerPostingType.SECURITY,
                        LedgerLegCardinality.AT_LEAST_ONE);
        assertLeg(maturity, LedgerLegRole.CASH_LEG, LedgerPostingType.CASH, LedgerLegCardinality.AT_LEAST_ONE);
        assertLeg(maturity, LedgerLegRole.PRINCIPAL_REDEMPTION_LEG, LedgerPostingType.PRINCIPAL_REDEMPTION,
                        LedgerLegCardinality.AT_LEAST_ONE);

        var conversion = LedgerEntryDefinitionRegistry
                        .lookup(LedgerEntryType.CORPORATE_ACTION, CorporateActionKind.CONVERSION)
                        .orElseThrow();
        assertLeg(conversion, LedgerLegRole.SOURCE_SECURITY_LEG, LedgerPostingType.SECURITY,
                        LedgerLegCardinality.AT_LEAST_ONE);
        assertLeg(conversion, LedgerLegRole.TARGET_SECURITY_LEG, LedgerPostingType.SECURITY,
                        LedgerLegCardinality.AT_LEAST_ONE);
    }

    /**
     * Checks the ledger rule scenario: rule vocabulary is consistent with posting type definitions.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testRuleVocabularyIsConsistentWithPostingTypeDefinitions()
    {
        for (var definition : LedgerEntryDefinitionRegistry.getDefinitions())
        {
            for (var rule : definition.getPostingRules())
            {
                assertTrue(rule.getPostingType().name(),
                                LedgerPostingTypeDefinitionRegistry.hasDefinition(rule.getPostingType()));

                for (var parameterType : rule.getRequiredParameterTypes())
                    assertTrue(parameterType.name(), definition.getPostingParameterTypes().contains(parameterType));

                for (var parameterType : rule.getOptionalParameterTypes())
                    assertTrue(parameterType.name(), definition.getPostingParameterTypes().contains(parameterType));
            }

            for (var rule : definition.getEntryParameterRules())
                assertTrue(rule.getParameterType().name(),
                                definition.getEntryParameterTypes().contains(rule.getParameterType()));

            for (var rule : definition.getPostingParameterRules())
                assertTrue(rule.getParameterType().name(),
                                definition.getPostingParameterTypes().contains(rule.getParameterType()));

            for (var rule : definition.getProjectionRules())
                assertTrue(rule.getRole().name(), definition.getProjectionRoles().contains(rule.getRole()));

            for (var leg : definition.getLegDefinitions())
            {
                assertTrue(leg.getPostingType().name(),
                                LedgerPostingTypeDefinitionRegistry.hasDefinition(leg.getPostingType()));

                for (var parameterType : leg.getRequiredParameterTypes())
                    assertTrue(parameterType.name(), definition.getPostingParameterTypes().contains(parameterType));

                for (var parameterType : leg.getOptionalParameterTypes())
                    assertTrue(parameterType.name(), definition.getPostingParameterTypes().contains(parameterType));

                leg.getProjectionRole().ifPresent(
                                role -> assertTrue(role.name(), definition.getProjectionRoles().contains(role)));
            }
        }
    }

    /**
     * Checks the ledger rule scenario: deep research critical vocabulary is covered by native definitions.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testDeepResearchCriticalVocabularyIsCoveredByNativeDefinitions()
    {
        var allPostingTypes = EnumSet.noneOf(LedgerPostingType.class);
        var allEntryParameters = EnumSet.noneOf(LedgerParameterType.class);
        var allPostingParameters = EnumSet.noneOf(LedgerParameterType.class);

        for (var definition : LedgerEntryDefinitionRegistry.getDefinitions())
        {
            allPostingTypes.addAll(definition.getPostingTypes());
            allEntryParameters.addAll(definition.getEntryParameterTypes());
            allPostingParameters.addAll(definition.getPostingParameterTypes());
        }

        assertTrue(allEntryParameters.contains(LedgerParameterType.CORPORATE_ACTION_KIND));
        assertTrue(allEntryParameters.contains(LedgerParameterType.EVENT_STAGE));
        assertTrue(allEntryParameters.contains(LedgerParameterType.RECORD_DATE));
        assertTrue(allEntryParameters.contains(LedgerParameterType.PAYMENT_DATE));
        assertTrue(allEntryParameters.contains(LedgerParameterType.EFFECTIVE_DATE));
        assertTrue(allPostingParameters.contains(LedgerParameterType.SOURCE_SECURITY));
        assertTrue(allPostingParameters.contains(LedgerParameterType.TARGET_SECURITY));
        assertTrue(allPostingParameters.contains(LedgerParameterType.RATIO_NUMERATOR));
        assertTrue(allPostingParameters.contains(LedgerParameterType.RATIO_DENOMINATOR));
        assertTrue(allPostingParameters.contains(LedgerParameterType.FRACTION_TREATMENT));
        assertTrue(allPostingParameters.contains(LedgerParameterType.CASH_IN_LIEU_AMOUNT));
        assertTrue(allPostingParameters.contains(LedgerParameterType.COST_ALLOCATION_METHOD));
        assertTrue(allPostingParameters.contains(LedgerParameterType.FAIR_MARKET_VALUE));
        assertTrue(allPostingParameters.contains(LedgerParameterType.FEE_REASON));
        assertTrue(allPostingParameters.contains(LedgerParameterType.TAX_REASON));
    }

    /**
     * Checks the ledger rule scenario: corporate action leg has controlled code domain.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testCorporateActionLegHasControlledCodeDomain()
    {
        assertThat(LedgerParameterType.CORPORATE_ACTION_LEG.getCodeDomain(),
                        is(LedgerParameterCodeDomain.CORPORATE_ACTION_LEG));
        assertTrue(LedgerParameterType.CORPORATE_ACTION_LEG
                        .supportsCode(CorporateActionLeg.SOURCE_SECURITY.getCode()));
        assertTrue(LedgerParameterType.CORPORATE_ACTION_LEG
                        .supportsCode(CorporateActionLeg.TARGET_SECURITY.getCode()));
        assertFalse(LedgerParameterType.CORPORATE_ACTION_LEG.supportsCode("SOURCE"));
    }

    /**
     * Checks the ledger rule scenario: definition layer allows partial native completeness.
     * Invalid entry shapes must be rejected before they can be stored.
     * This keeps higher-level Ledger-V6 transaction flows predictable.
     */
    @Test
    public void testDefinitionLayerAllowsPartialNativeCompleteness()
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
        entry.addPosting(posting);
        ledger.addEntry(entry);

        assertTrue(LedgerStructuralValidator.validate(ledger).isOK());
    }

    private void assertOptionalPosting(name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition,
                    LedgerPostingType postingType)
    {
        assertTrue(postingType.name(), hasPostingRule(definition.getOptionalPostingRules(), postingType));
    }

    private void assertRequiredEntryParameter(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition,
                    LedgerParameterType parameterType)
    {
        assertTrue(parameterType.name(), hasParameterRule(definition.getRequiredEntryParameterRules(), parameterType));
    }

    private void assertOptionalEntryParameter(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition,
                    LedgerParameterType parameterType)
    {
        assertTrue(parameterType.name(), hasParameterRule(definition.getOptionalEntryParameterRules(), parameterType));
    }

    private void assertRequiredPostingParameter(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition,
                    LedgerParameterType parameterType)
    {
        assertTrue(parameterType.name(), hasParameterRule(definition.getRequiredPostingParameterRules(), parameterType));
    }

    private void assertOptionalPostingParameter(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition,
                    LedgerParameterType parameterType)
    {
        assertTrue(parameterType.name(), hasParameterRule(definition.getOptionalPostingParameterRules(), parameterType));
    }

    private void assertRepeatableParameter(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition,
                    LedgerParameterType parameterType)
    {
        assertTrue(parameterType.name(), definition.getRepeatableParameterTypes().contains(parameterType));
    }

    private void assertOptionalProjection(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition,
                    LedgerProjectionRole role, boolean primaryPostingExpected, boolean postingGroupExpected)
    {
        assertProjection(definition.getOptionalProjectionRules(), role, primaryPostingExpected, postingGroupExpected);
    }

    private void assertProjection(Iterable<LedgerProjectionRule> rules, LedgerProjectionRole role,
                    boolean primaryPostingExpected, boolean postingGroupExpected)
    {
        for (var rule : rules)
        {
            if (rule.getRole() == role)
            {
                assertThat(rule.isPrimaryPostingExpected(), is(primaryPostingExpected));
                assertThat(rule.isPostingGroupExpected(), is(postingGroupExpected));
                return;
            }
        }

        assertTrue(role.name(), false);
    }

    private void assertAlternativeGroup(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition, String name,
                    LedgerRequirement requirement, LedgerParameterType first, LedgerParameterType... rest)
    {
        var expected = EnumSet.of(first, rest);

        for (var group : definition.getAlternativeRequirementGroups())
        {
            if (group.getName().equals(name))
            {
                assertThat(group.getRequirement(), is(requirement));
                assertThat(group.getParameterTypes(), is(expected));
                return;
            }
        }

        assertTrue(name, false);
    }

    private boolean hasPostingRule(Iterable<LedgerPostingRule> rules, LedgerPostingType postingType)
    {
        for (var rule : rules)
            if (rule.getPostingType() == postingType)
                return true;

        return false;
    }

    private boolean hasParameterRule(Iterable<LedgerParameterRule> rules, LedgerParameterType parameterType)
    {
        for (var rule : rules)
            if (rule.getParameterType() == parameterType)
                return true;

        return false;
    }

    private void assertUniquePostingRules(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition)
    {
        var seen = new HashSet<LedgerPostingType>();

        for (var rule : definition.getPostingRules())
            assertTrue(definition.getEntryType() + ": posting rule " + rule.getPostingType(),
                            seen.add(rule.getPostingType()));
    }

    private void assertUniqueParameterRules(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition,
                    Iterable<LedgerParameterRule> rules, String category)
    {
        var seen = new HashSet<LedgerParameterType>();

        for (var rule : rules)
            assertTrue(definition.getEntryType() + ": " + category + " " + rule.getParameterType(),
                            seen.add(rule.getParameterType()));
    }

    private void assertUniqueProjectionRules(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition)
    {
        var seen = new HashSet<LedgerProjectionRole>();

        for (var rule : definition.getProjectionRules())
            assertTrue(definition.getEntryType() + ": projection rule " + rule.getRole(), seen.add(rule.getRole()));
    }

    private void assertUniquePostingGroupRules(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition)
    {
        var seen = new HashSet<String>();

        for (var rule : definition.getPostingGroupRules())
            assertTrue(definition.getEntryType() + ": posting group rule " + rule.getName(), seen.add(rule.getName()));
    }

    private void assertUniqueAlternativeGroupRules(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition)
    {
        var seen = new HashSet<String>();

        for (var group : definition.getAlternativeRequirementGroups())
            assertTrue(definition.getEntryType() + ": alternative group " + group.getName(), seen.add(group.getName()));
    }

    private void assertUniqueLegDefinitions(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition)
    {
        var seen = new HashSet<LedgerLegRole>();

        for (var leg : definition.getLegDefinitions())
            assertTrue(definition.getEntryType() + ": leg " + leg.getRole(), seen.add(leg.getRole()));
    }

    private LedgerLegDefinition assertLeg(
                    name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinition definition,
                    LedgerLegRole role, LedgerPostingType postingType, LedgerLegCardinality cardinality)
    {
        var leg = definition.getLegDefinition(role).orElseThrow();

        assertThat(leg.getPostingType(), is(postingType));
        assertThat(leg.getCardinality(), is(cardinality));

        return leg;
    }
}
