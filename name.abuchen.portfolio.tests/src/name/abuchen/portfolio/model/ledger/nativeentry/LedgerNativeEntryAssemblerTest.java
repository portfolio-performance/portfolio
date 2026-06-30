package name.abuchen.portfolio.model.ledger.nativeentry;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.Ledger;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;
import name.abuchen.portfolio.model.ledger.configuration.CashCompensationKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionSubtype;
import name.abuchen.portfolio.model.ledger.configuration.EventStage;
import name.abuchen.portfolio.model.ledger.configuration.FeeReason;
import name.abuchen.portfolio.model.ledger.configuration.FractionTreatment;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryDefinitionRegistry;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPerformanceTreatment;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerReportingClass;
import name.abuchen.portfolio.model.ledger.configuration.RoundingModeCode;
import name.abuchen.portfolio.model.ledger.configuration.TaxReason;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests ledger-native entry assembly for advanced transaction shapes.
 * These tests make sure structural facts can be represented without enabling unsupported UI workflows.
 */
@SuppressWarnings("nls")
public class LedgerNativeEntryAssemblerTest
{
    /**
     * Checks the Ledger-V6 scenario: rejects legacy fixed shape entry type.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testRejectsLegacyFixedShapeEntryType()
    {
        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> LedgerNativeEntryAssembler.forClient(new Client()).forType(LedgerEntryType.BUY));

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.ENTRY_TYPE_NOT_NATIVE));
        assertThat(exception.getMessage(), containsString("Use LedgerTransactionCreator for standard transaction families"));
    }

    /**
     * Checks the Ledger-V6 scenario: for type accepts every defined ledger native entry type.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testForTypeAcceptsEveryDefinedLedgerNativeEntryType()
    {
        var assembler = LedgerNativeEntryAssembler.forClient(new Client());

        for (var definition : LedgerEntryDefinitionRegistry.getDefinitions())
        {
            assertTrue(definition.getEntryType().isLedgerNativeTargeted());
            assertThat(assembler.forType(definition.getEntryType()), is(notNullValue()));
        }
    }

    /**
     * Checks the Ledger-V6 scenario: for type rejects every legacy fixed shape entry type.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testForTypeRejectsEveryLegacyFixedShapeEntryType()
    {
        var assembler = LedgerNativeEntryAssembler.forClient(new Client());

        for (var entryType : LedgerEntryType.values())
        {
            if (entryType.isLegacyFixedShape())
            {
                var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                                () -> assembler.forType(entryType));

                assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.ENTRY_TYPE_NOT_NATIVE));
            }
        }
    }

    /**
     * Checks the Ledger-V6 scenario: spin off is convenience for for type.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testSpinOffIsConvenienceForForType()
    {
        var fixture = fixture();

        assertThat(LedgerNativeEntryAssembler.forClient(fixture.client).spinOff(), is(notNullValue()));
        assertThat(LedgerNativeEntryAssembler.forClient(fixture.client).forType(LedgerEntryType.SPIN_OFF),
                        is(notNullValue()));
    }

    /**
     * Checks the Ledger-V6 scenario: definition registry schemas are readable by assembler consumers.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testDefinitionRegistrySchemasAreReadableByAssemblerConsumers()
    {
        for (var definition : LedgerEntryDefinitionRegistry.getDefinitions())
        {
            assertThat(definition.getEntryType(), is(notNullValue()));
            assertThat(definition.getPostingRules().isEmpty(), is(false));
            assertThat(definition.getEntryParameterRules().isEmpty(), is(false));
            assertThat(definition.getPostingParameterRules().isEmpty(), is(false));
            assertThat(definition.getProjectionRules().isEmpty(), is(false));
            assertThat(definition.getPostingGroupRules().isEmpty(), is(false));
            assertThat(definition.getAlternativeRequirementGroups().isEmpty(), is(false));
            assertThat(definition.getReportingClass() != LedgerReportingClass.UNDEFINED, is(true));
            assertThat(definition.getPerformanceTreatment() != LedgerPerformanceTreatment.UNDEFINED, is(true));
            assertThat(definition.getDownstreamResultsNotPersisted().isEmpty(), is(false));
        }
    }

    /**
     * Checks the Ledger-V6 scenario: rejects missing entry definition.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testRejectsMissingEntryDefinition()
    {
        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> new LedgerNativeEntryAssembler(new Client(), type -> Optional.empty())
                                        .forType(LedgerEntryType.SPIN_OFF));

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.ENTRY_DEFINITION_MISSING));
    }

    /**
     * Checks the Ledger-V6 scenario: rejects posting type not in entry definition.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testRejectsPostingTypeNotInEntryDefinition()
    {
        var fixture = fixture();
        var invalidLeg = NativeSecurityLeg.ofType(LedgerPostingType.BOND) //
                        .portfolio(fixture.portfolio) //
                        .security(fixture.siemens) //
                        .shares(Values.Share.factorize(1)) //
                        .amount(money(1)) //
                        .build();

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture).securityLeg(invalidLeg).buildDetached());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.POSTING_TYPE_NOT_IN_ENTRY_DEFINITION));
    }

    /**
     * Checks the Ledger-V6 scenario: rejects posting type outside each entry definition.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testRejectsPostingTypeOutsideEachEntryDefinition()
    {
        var fixture = fixture();

        for (var definition : LedgerEntryDefinitionRegistry.getDefinitions())
        {
            var invalidPostingType = java.util.Arrays.stream(LedgerPostingType.values()) //
                            .filter(postingType -> !definition.getPostingTypes().contains(postingType)) //
                            .findFirst().orElseThrow();
            var invalidLeg = NativeSecurityLeg.ofType(invalidPostingType) //
                            .portfolio(fixture.portfolio) //
                            .security(fixture.siemens) //
                            .shares(Values.Share.factorize(1)) //
                            .amount(money(1)) //
                            .build();

            var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                            () -> LedgerNativeEntryAssembler.forClient(fixture.client)
                                            .forType(definition.getEntryType()) //
                                            .metadata(metadata()) //
                                            .event(event(definition.getEntryType())) //
                                            .securityLeg(invalidLeg) //
                                            .buildDetached());

            assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.POSTING_TYPE_NOT_IN_ENTRY_DEFINITION));
        }
    }

    /**
     * Checks the Ledger-V6 scenario: rejects entry parameter not allowed by entry definition.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testRejectsEntryParameterNotAllowedByEntryDefinition()
    {
        var fixture = fixture();
        var event = NativeCorporateActionEvent.builder() //
                        .kind(CorporateActionKind.SPIN_OFF) //
                        .parameter(LedgerParameterType.SOURCE_ACCOUNT, fixture.account) //
                        .build();

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture).event(event).buildDetached());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.PARAMETER_NOT_IN_ENTRY_DEFINITION));
    }

    /**
     * Checks the Ledger-V6 scenario: rejects posting parameter not meaningful for posting type definition.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testRejectsPostingParameterNotMeaningfulForPostingTypeDefinition()
    {
        var fixture = fixture();
        var sourceLeg = sourceLeg(fixture).parameter(LedgerParameterType.CASH_ACCOUNT, fixture.account).build();

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture).securityLeg(sourceLeg).buildDetached());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.PARAMETER_NOT_IN_POSTING_TYPE_DEFINITION));
    }

    /**
     * Checks the Ledger-V6 scenario: rejects projection role not allowed by entry definition.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testRejectsProjectionRoleNotAllowedByEntryDefinition()
    {
        var fixture = fixture();
        var stockDividendLeg = NativeSecurityLeg.target() //
                        .portfolio(fixture.portfolio) //
                        .security(fixture.siemensEnergy) //
                        .shares(Values.Share.factorize(5)) //
                        .amount(money(50)) //
                        .targetSecurity(fixture.siemensEnergy) //
                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2))) //
                        .projectAs(LedgerProjectionRole.OLD_SECURITY_LEG) //
                        .build();

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> LedgerNativeEntryAssembler.forClient(fixture.client) //
                                        .forType(LedgerEntryType.STOCK_DIVIDEND) //
                                        .metadata(metadata()) //
                                        .event(event(LedgerEntryType.STOCK_DIVIDEND)) //
                                        .securityLeg(stockDividendLeg) //
                                        .buildDetached());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.PROJECTION_ROLE_NOT_IN_ENTRY_DEFINITION));
    }

    /**
     * Checks the Ledger-V6 scenario: rejects wrong value kind carrier.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testRejectsWrongValueKindCarrier()
    {
        var fixture = fixture();
        var event = NativeCorporateActionEvent.builder() //
                        .kind(CorporateActionKind.SPIN_OFF) //
                        .parameter(LedgerParameterType.EFFECTIVE_DATE, "2020-09-28") //
                        .build();

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture).event(event).buildDetached());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.VALUE_KIND_MISMATCH));
    }

    /**
     * Checks the Ledger-V6 scenario: rejects invalid code domain value.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testRejectsInvalidCodeDomainValue()
    {
        var fixture = fixture();
        var event = NativeCorporateActionEvent.builder().kind("SOURCE").build();

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture).event(event).buildDetached());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.PARAMETER_CODE_NOT_ALLOWED));
    }

    /**
     * Checks the Ledger-V6 scenario: builds detached minimal spin off.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testBuildsDetachedMinimalSpinOff()
    {
        var fixture = fixture();
        var result = validSpinOff(fixture).buildDetached();
        var entry = result.getEntry();

        assertThat(entry.getType(), is(LedgerEntryType.SPIN_OFF));
        assertThat(entry.getPostings().size(), is(5));
        assertThat(entry.getProjectionRefs().size(), is(3));
        assertThat(fixture.client.getLedger().getEntries().size(), is(0));
        assertTrue(result.getValidationResult().isOK());

        assertThat(parameter(entry.getParameters(), LedgerParameterType.CORPORATE_ACTION_KIND).getValue(),
                        is(CorporateActionKind.SPIN_OFF.getCode()));
        assertThat(entry.getProjectionRefs().stream().filter(ref -> ref.getRole() == LedgerProjectionRole.OLD_SECURITY_LEG)
                        .count(), is(1L));
        assertThat(entry.getProjectionRefs().stream().filter(ref -> ref.getRole() == LedgerProjectionRole.NEW_SECURITY_LEG)
                        .count(), is(1L));
        assertThat(entry.getProjectionRefs().stream()
                        .filter(ref -> ref.getRole() == LedgerProjectionRole.CASH_COMPENSATION).count(), is(1L));
    }

    /**
     * Checks the Ledger-V6 scenario: build detached does not mutate client.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testBuildDetachedDoesNotMutateClient()
    {
        var fixture = fixture();

        validSpinOff(fixture).buildDetached();

        assertThat(fixture.client.getLedger().getEntries().size(), is(0));
        assertThat(fixture.account.getTransactions().size(), is(0));
        assertThat(fixture.portfolio.getTransactions().size(), is(0));
    }

    /**
     * Checks the Ledger-V6 scenario: builds detached minimal stock dividend through generic for type.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testBuildsDetachedMinimalStockDividendThroughGenericForType()
    {
        var fixture = fixture();
        var stockDividendLeg = NativeSecurityLeg.target() //
                        .portfolio(fixture.portfolio) //
                        .security(fixture.siemensEnergy) //
                        .shares(Values.Share.factorize(5)) //
                        .amount(money(50)) //
                        .targetSecurity(fixture.siemensEnergy) //
                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2))) //
                        .projectAs(LedgerProjectionRole.DELIVERY_INBOUND) //
                        .build();

        var result = LedgerNativeEntryAssembler.forClient(fixture.client) //
                        .forType(LedgerEntryType.STOCK_DIVIDEND) //
                        .metadata(metadata()) //
                        .event(event(LedgerEntryType.STOCK_DIVIDEND)) //
                        .securityLeg(stockDividendLeg) //
                        .buildDetached();

        assertThat(result.getEntry().getType(), is(LedgerEntryType.STOCK_DIVIDEND));
        assertThat(result.getEntry().getProjectionRefs().get(0).getRole(), is(LedgerProjectionRole.DELIVERY_INBOUND));
        assertTrue(result.getValidationResult().isOK());
        assertThat(fixture.client.getLedger().getEntries().size(), is(0));
    }

    /**
     * Checks the Ledger-V6 scenario: build and add creates spin off and materializes runtime projections.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testBuildAndAddCreatesSpinOffAndMaterializesRuntimeProjections()
    {
        var fixture = fixture();
        var result = validSpinOff(fixture).buildAndAdd();
        var entry = result.getEntry();

        assertThat(fixture.client.getLedger().getEntries().size(), is(1));
        assertThat(fixture.client.getLedger().getEntries().get(0), is(entry));
        assertTrue(result.getValidationResult().isOK());
        assertTrue(LedgerStructuralValidator.validate(fixture.client.getLedger()).isOK());

        assertThat(fixture.portfolio.getTransactions().size(), is(2));
        assertThat(fixture.account.getTransactions().size(), is(1));
        assertThat(portfolioProjection(fixture.portfolio, LedgerProjectionRole.OLD_SECURITY_LEG).getLedgerEntry(),
                        is(entry));
        assertThat(portfolioProjection(fixture.portfolio, LedgerProjectionRole.NEW_SECURITY_LEG).getLedgerEntry(),
                        is(entry));
        assertThat(accountProjection(fixture.account, LedgerProjectionRole.CASH_COMPENSATION).getLedgerEntry(),
                        is(entry));
    }

    /**
     * Checks the Ledger-V6 scenario: build and add projection ref uuids are runtime projection uuids.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testBuildAndAddProjectionRefUUIDsAreRuntimeProjectionUUIDs()
    {
        var fixture = fixture();
        var entry = validSpinOff(fixture).buildAndAdd().getEntry();
        var projectionUUIDs = entry.getProjectionRefs().stream().map(ref -> ref.getUUID()).collect(Collectors.toSet());
        var runtimeUUIDs = java.util.stream.Stream.concat(
                        fixture.portfolio.getTransactions().stream()
                                        .filter(LedgerBackedTransaction.class::isInstance)
                                        .map(PortfolioTransaction::getUUID),
                        fixture.account.getTransactions().stream()
                                        .filter(LedgerBackedTransaction.class::isInstance)
                                        .map(AccountTransaction::getUUID))
                        .collect(Collectors.toSet());

        assertThat(runtimeUUIDs, is(projectionUUIDs));
        assertThat(runtimeUUIDs.size(), is(3));
    }

    /**
     * Checks the Ledger-V6 scenario: build and add does not duplicate runtime projections.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testBuildAndAddDoesNotDuplicateRuntimeProjections()
    {
        var fixture = fixture();

        validSpinOff(fixture).buildAndAdd();
        validSpinOff(fixture).buildAndAdd();

        assertThat(fixture.client.getLedger().getEntries().size(), is(2));
        assertThat(fixture.portfolio.getTransactions().size(), is(4));
        assertThat(fixture.account.getTransactions().size(), is(2));
        assertThat(runtimeProjectionUUIDs(fixture).size(), is(6));
    }

    /**
     * Checks the Ledger-V6 scenario: build and add invalid code domain leaves client unchanged.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testBuildAndAddInvalidCodeDomainLeavesClientUnchanged()
    {
        var fixture = fixture();
        var event = NativeCorporateActionEvent.builder().kind("SOURCE").build();

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture).event(event).buildAndAdd());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.PARAMETER_CODE_NOT_ALLOWED));
        assertClientUnchanged(fixture);
    }

    /**
     * Checks the Ledger-V6 scenario: build and add invalid posting type leaves client unchanged.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testBuildAndAddInvalidPostingTypeLeavesClientUnchanged()
    {
        var fixture = fixture();
        var invalidLeg = NativeSecurityLeg.ofType(LedgerPostingType.BOND) //
                        .portfolio(fixture.portfolio) //
                        .security(fixture.siemens) //
                        .shares(Values.Share.factorize(1)) //
                        .amount(money(1)) //
                        .build();

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture).securityLeg(invalidLeg).buildAndAdd());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.POSTING_TYPE_NOT_IN_ENTRY_DEFINITION));
        assertClientUnchanged(fixture);
    }

    /**
     * Checks the Ledger-V6 scenario: build and add structural validation failure leaves client unchanged.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testBuildAndAddStructuralValidationFailureLeavesClientUnchanged()
    {
        var fixture = fixture();
        var invalidSourceLeg = sourceLeg(fixture).portfolio(null).build();

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture).securityLeg(invalidSourceLeg).buildAndAdd());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.STRUCTURAL_VALIDATION_FAILED));
        assertClientUnchanged(fixture);
    }

    /**
     * Checks the Ledger-V6 scenario: generated detached entry passes structural validator when added to scratch ledger.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testGeneratedDetachedEntryPassesStructuralValidatorWhenAddedToScratchLedger()
    {
        var fixture = fixture();
        var result = validSpinOff(fixture).buildDetached();
        var ledger = new Ledger();

        ledger.addEntry(result.getEntry());

        assertTrue(LedgerStructuralValidator.validate(ledger).isOK());
    }

    /**
     * Checks the Ledger-V6 scenario: generated projection refs target assembler owned postings.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testGeneratedProjectionRefsTargetAssemblerOwnedPostings()
    {
        var fixture = fixture();
        var entry = validSpinOff(fixture).buildDetached().getEntry();
        var postingUUIDs = entry.getPostings().stream().map(LedgerPosting::getUUID).collect(Collectors.toSet());

        for (var ref : entry.getProjectionRefs())
        {
            assertTrue(postingUUIDs.contains(ref.getPrimaryPostingUUID()));
            assertThat(ref.getPrimaryMembership().orElseThrow().getPostingUUID(), is(ref.getPrimaryPostingUUID()));

            if (ref.getPostingGroupUUID() != null)
            {
                assertTrue(postingUUIDs.contains(ref.getPostingGroupUUID()));
                assertThat(ref.getMembershipsByRole(ProjectionMembershipRole.GROUP_ANCHOR).get(0).getPostingUUID(),
                                is(ref.getPostingGroupUUID()));
            }
        }
    }

    private static LedgerNativeEntryAssembler.EntryBuilder validSpinOff(Fixture fixture)
    {
        return baseSpinOff(fixture) //
                        .securityLeg(sourceLeg(fixture).build()) //
                        .securityLeg(targetLeg(fixture).build()) //
                        .cashCompensation(NativeCashCompensation.builder() //
                                        .account(fixture.account) //
                                        .amount(money(5)) //
                                        .kind(CashCompensationKind.CASH_IN_LIEU) //
                                        .applied(true) //
                                        .fractionQuantity(new BigDecimal("0.5")) //
                                        .fractionTreatment(FractionTreatment.CASH_IN_LIEU) //
                                        .roundingMode(RoundingModeCode.FLOOR) //
                                        .build()) //
                        .fee(NativeFee.of(fixture.account, money(2),
                                        FeeReason.CORPORATE_ACTION_FEE)) //
                        .tax(NativeTax.builder() //
                                        .account(fixture.account) //
                                        .amount(money(1)) //
                                        .reason(TaxReason.WITHHOLDING_TAX) //
                                        .taxableDistribution(true) //
                                        .withholdingTax(true) //
                                        .transactionTax(false) //
                                        .reclaimableTax(false) //
                                        .build());
    }

    private static LedgerNativeEntryAssembler.EntryBuilder baseSpinOff(Fixture fixture)
    {
        return LedgerNativeEntryAssembler.forClient(fixture.client).spinOff() //
                        .metadata(metadata()) //
                        .event(event(LedgerEntryType.SPIN_OFF));
    }

    private static NativeEntryMetadata metadata()
    {
        return NativeEntryMetadata.of(LocalDateTime.of(2020, 9, 28, 0, 0)) //
                        .note("Native corporate action") //
                        .source("native-entry-assembler-test");
    }

    private static NativeCorporateActionEvent event(LedgerEntryType entryType)
    {
        return NativeCorporateActionEvent.builder() //
                        .kind(corporateActionKind(entryType)) //
                        .subtype(CorporateActionSubtype.STANDARD) //
                        .reference(entryType.name() + "-2020") //
                        .stage(EventStage.SETTLED) //
                        .effectiveDate(LocalDate.of(2020, 9, 28)) //
                        .build();
    }

    private static CorporateActionKind corporateActionKind(LedgerEntryType entryType)
    {
        for (var kind : CorporateActionKind.values())
            if (kind.getRelatedEntryType().filter(entryType::equals).isPresent())
                return kind;

        throw new IllegalArgumentException("No corporate action kind for " + entryType);
    }

    private static NativeSecurityLeg.Builder sourceLeg(Fixture fixture)
    {
        return NativeSecurityLeg.source() //
                        .portfolio(fixture.portfolio) //
                        .security(fixture.siemens) //
                        .shares(Values.Share.factorize(10)) //
                        .amount(money(100)) //
                        .sourceSecurity(fixture.siemens) //
                        .targetSecurity(fixture.siemensEnergy) //
                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2)));
    }

    private static NativeSecurityLeg.Builder targetLeg(Fixture fixture)
    {
        return NativeSecurityLeg.target() //
                        .portfolio(fixture.portfolio) //
                        .security(fixture.siemensEnergy) //
                        .shares(Values.Share.factorize(5)) //
                        .amount(money(50)) //
                        .sourceSecurity(fixture.siemens) //
                        .targetSecurity(fixture.siemensEnergy) //
                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2)));
    }

    private static LedgerParameter<?> parameter(Collection<LedgerParameter<?>> parameters, LedgerParameterType type)
    {
        return parameters.stream().filter(parameter -> parameter.getType() == type).findFirst().orElseThrow();
    }

    private static LedgerBackedTransaction portfolioProjection(Portfolio portfolio, LedgerProjectionRole role)
    {
        return portfolio.getTransactions().stream() //
                        .filter(LedgerBackedTransaction.class::isInstance) //
                        .map(LedgerBackedTransaction.class::cast) //
                        .filter(transaction -> transaction.getLedgerProjectionRef().getRole() == role) //
                        .findFirst().orElseThrow();
    }

    private static LedgerBackedTransaction accountProjection(Account account, LedgerProjectionRole role)
    {
        return account.getTransactions().stream() //
                        .filter(LedgerBackedTransaction.class::isInstance) //
                        .map(LedgerBackedTransaction.class::cast) //
                        .filter(transaction -> transaction.getLedgerProjectionRef().getRole() == role) //
                        .findFirst().orElseThrow();
    }

    private static java.util.Set<String> runtimeProjectionUUIDs(Fixture fixture)
    {
        return java.util.stream.Stream.concat(
                        fixture.portfolio.getTransactions().stream()
                                        .filter(LedgerBackedTransaction.class::isInstance)
                                        .map(PortfolioTransaction::getUUID),
                        fixture.account.getTransactions().stream()
                                        .filter(LedgerBackedTransaction.class::isInstance)
                                        .map(AccountTransaction::getUUID))
                        .collect(Collectors.toSet());
    }

    private static void assertClientUnchanged(Fixture fixture)
    {
        assertThat(fixture.client.getLedger().getEntries().size(), is(0));
        assertThat(fixture.account.getTransactions().size(), is(0));
        assertThat(fixture.portfolio.getTransactions().size(), is(0));
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
        var siemens = new Security("Siemens AG", CurrencyUnit.EUR);
        var siemensEnergy = new Security("Siemens Energy AG", CurrencyUnit.EUR);

        account.setName("Cash");
        account.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setName("Portfolio");
        siemens.setIsin("DE0007236101");
        siemensEnergy.setIsin("DE000ENER6Y0");

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(siemens);
        client.addSecurity(siemensEnergy);

        return new Fixture(client, account, portfolio, siemens, siemensEnergy);
    }

    private static final class Fixture
    {
        private final Client client;
        private final Account account;
        private final Portfolio portfolio;
        private final Security siemens;
        private final Security siemensEnergy;

        private Fixture(Client client, Account account, Portfolio portfolio, Security siemens, Security siemensEnergy)
        {
            this.client = client;
            this.account = account;
            this.portfolio = portfolio;
            this.siemens = siemens;
            this.siemensEnergy = siemensEnergy;
        }
    }
}
