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
import name.abuchen.portfolio.model.ledger.LedgerPostingDirection;
import name.abuchen.portfolio.model.ledger.LedgerPostingSemanticRole;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.configuration.CashCompensationKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
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
import name.abuchen.portfolio.model.ledger.projection.DerivedProjectionDescriptor;
import name.abuchen.portfolio.model.ledger.projection.DerivedProjectionDescriptorService;
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
        assertThat(LedgerNativeEntryAssembler.forClient(fixture.client).forType(LedgerEntryType.CORPORATE_ACTION),
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
                                        .forType(LedgerEntryType.CORPORATE_ACTION));

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
            if (definition.getProjectionRules().isEmpty())
                continue;

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
                                            .event(event()) //
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
        var targetLeg = targetLeg(fixture).projectAs(LedgerProjectionRole.SOURCE_ACCOUNT).build();

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture).securityLeg(targetLeg).buildDetached());

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

        assertThat(entry.getType(), is(LedgerEntryType.CORPORATE_ACTION));
        assertThat(entry.getPostings().size(), is(6));
        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).size(), is(3));
        assertThat(fixture.client.getLedger().getEntries().size(), is(0));
        assertTrue(result.getValidationResult().isOK());

        assertThat(parameter(entry.getParameters(), LedgerParameterType.CORPORATE_ACTION_KIND).getValue(),
                        is(CorporateActionKind.SPIN_OFF.getCode()));
        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).stream().filter(ref -> ref.getRole() == LedgerProjectionRole.OLD_SECURITY_LEG)
                        .count(), is(1L));
        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).stream().filter(ref -> ref.getRole() == LedgerProjectionRole.NEW_SECURITY_LEG)
                        .count(), is(1L));
        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).stream()
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

    @Test
    public void testNativeAssemblerEmitsSemanticPostingsForSiemensSpinOff()
    {
        var fixture = fixture();
        var entry = spinOffWithRetainedAndNewLegs(fixture).buildDetached().getEntry();
        var oldLeg = descriptor(entry, LedgerProjectionRole.OLD_SECURITY_LEG).getPrimaryPosting();
        var retainedLeg = descriptor(entry, LedgerProjectionRole.DELIVERY_INBOUND).getPrimaryPosting();
        var newLeg = descriptor(entry, LedgerProjectionRole.NEW_SECURITY_LEG).getPrimaryPosting();
        var compensation = descriptor(entry, LedgerProjectionRole.CASH_COMPENSATION).getPrimaryPosting();

        assertPrimarySemantics(oldLeg, LedgerPostingSemanticRole.SECURITY, LedgerPostingDirection.OUTBOUND,
                        CorporateActionLeg.SOURCE_SECURITY, LedgerProjectionRole.OLD_SECURITY_LEG);
        assertPrimarySemantics(retainedLeg, LedgerPostingSemanticRole.SECURITY, LedgerPostingDirection.INBOUND,
                        CorporateActionLeg.TARGET_SECURITY, LedgerProjectionRole.DELIVERY_INBOUND);
        assertPrimarySemantics(newLeg, LedgerPostingSemanticRole.SECURITY, LedgerPostingDirection.INBOUND,
                        CorporateActionLeg.TARGET_SECURITY, LedgerProjectionRole.NEW_SECURITY_LEG);
        assertPrimarySemantics(compensation, LedgerPostingSemanticRole.CASH_COMPENSATION,
                        LedgerPostingDirection.NEUTRAL, CorporateActionLeg.CASH_COMPENSATION,
                        LedgerProjectionRole.CASH_COMPENSATION);
        assertThat(entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.FEE)
                        .findFirst().orElseThrow().getGroupKey(), is(LedgerProjectionRole.CASH_COMPENSATION.name()));
        assertThat(entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.TAX)
                        .findFirst().orElseThrow().getUnitRole(), is(LedgerPostingUnitRole.TAX));
    }

    @Test
    public void testNativeAssemblerCreatesRepeatedSpinOffMovementLegsWithSemanticKeys()
    {
        var fixture = fixture();
        var secondTarget = new Security("Siemens Healthineers AG", CurrencyUnit.EUR);
        secondTarget.setIsin("DE000SHL1006");
        fixture.client.addSecurity(secondTarget);

        var entry = repeatedSpinOff(fixture, secondTarget).buildDetached().getEntry();
        var targetPostings = postings(entry, LedgerPostingType.SECURITY, CorporateActionLeg.TARGET_SECURITY);
        var cashPostings = postings(entry, LedgerPostingType.CASH_COMPENSATION,
                        CorporateActionLeg.CASH_COMPENSATION);
        var targetDescriptors = descriptors(entry).stream()
                        .filter(descriptor -> descriptor.getRole() == LedgerProjectionRole.NEW_SECURITY_LEG)
                        .toList();
        var cashOneDescriptor = descriptor(entry, LedgerProjectionRole.CASH_COMPENSATION, "cash-1");
        var cashTwoDescriptor = descriptor(entry, LedgerProjectionRole.CASH_COMPENSATION, "cash-2");

        assertThat(targetPostings.size(), is(2));
        assertThat(targetPostings.stream().map(LedgerPosting::getLocalKey).collect(Collectors.toSet()),
                        is(java.util.Set.of("target-1", "target-2")));
        assertThat(targetPostings.stream().map(LedgerPosting::getGroupKey).collect(Collectors.toSet()),
                        is(java.util.Set.of("main")));

        assertThat(cashPostings.size(), is(2));
        assertThat(cashPostings.stream().map(LedgerPosting::getLocalKey).collect(Collectors.toSet()),
                        is(java.util.Set.of("cash-1", "cash-2")));
        assertThat(cashPostings.stream().map(LedgerPosting::getGroupKey).collect(Collectors.toSet()),
                        is(java.util.Set.of("cash-1", "cash-2")));

        assertThat(targetDescriptors.size(), is(2));
        assertThat(targetDescriptors.stream().map(DerivedProjectionDescriptor::getSemanticInstanceKey)
                        .map(Optional::orElseThrow).collect(Collectors.toSet()),
                        is(java.util.Set.of("target-1", "target-2")));
        assertThat(targetDescriptors.stream().map(DerivedProjectionDescriptor::getRuntimeProjectionId)
                        .collect(Collectors.toSet()).size(), is(2));
        assertThat(descriptor(entry, LedgerProjectionRole.NEW_SECURITY_LEG, "target-1").getPrimaryPosting()
                        .getSecurity(), is(fixture.siemensEnergy));
        assertThat(descriptor(entry, LedgerProjectionRole.NEW_SECURITY_LEG, "target-2").getPrimaryPosting()
                        .getSecurity(), is(secondTarget));

        assertThat(cashOneDescriptor.getUnitPostings().stream().map(LedgerPosting::getLocalKey)
                        .collect(Collectors.toSet()), is(java.util.Set.of("fee-1", "tax-1")));
        assertThat(cashTwoDescriptor.getUnitPostings().isEmpty(), is(true));
        assertTrue(LedgerStructuralValidator.validate(ledger(entry)).isOK());
    }

    @Test
    public void testNativeAssemblerRepeatedTargetDuplicateLocalKeyIsRejected()
    {
        var fixture = fixture();
        var secondTarget = new Security("Siemens Healthineers AG", CurrencyUnit.EUR);
        fixture.client.addSecurity(secondTarget);

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture) //
                                        .securityLeg(sourceLeg(fixture).build()) //
                                        .securityLeg(targetLeg(fixture).groupKey("main").localKey("target-1").build()) //
                                        .securityLeg(targetLeg(fixture).security(secondTarget)
                                                        .targetSecurity(secondTarget).groupKey("main")
                                                        .localKey("target-1").build()) //
                                        .buildDetached());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.NATIVE_DEFINITION_VALIDATION_FAILED));
    }

    @Test
    public void testNativeAssemblerRepeatedTargetBlankLocalKeyIsRejected()
    {
        var fixture = fixture();
        var secondTarget = new Security("Siemens Healthineers AG", CurrencyUnit.EUR);
        fixture.client.addSecurity(secondTarget);

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture) //
                                        .securityLeg(sourceLeg(fixture).build()) //
                                        .securityLeg(targetLeg(fixture).groupKey("main").localKey("target-1").build()) //
                                        .securityLeg(targetLeg(fixture).security(secondTarget)
                                                        .targetSecurity(secondTarget).groupKey("main")
                                                        .localKey(" ").build()) //
                                        .buildDetached());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.NATIVE_DEFINITION_VALIDATION_FAILED));
    }

    @Test
    public void testNativeAssemblerRepeatedCashDuplicateLocalKeyIsRejected()
    {
        var fixture = fixture();

        var exception = assertThrows(LedgerNativeEntryAssemblyException.class,
                        () -> baseSpinOff(fixture) //
                                        .securityLeg(sourceLeg(fixture).build()) //
                                        .securityLeg(targetLeg(fixture).build()) //
                                        .cashCompensation(cashCompensation(fixture, "cash-1", "cash-1", 5)) //
                                        .cashCompensationMovement(cashCompensation(fixture, "cash-2", "cash-1", 7)) //
                                        .buildDetached());

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.NATIVE_DEFINITION_VALIDATION_FAILED));
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
     * Checks the Ledger-V6 scenario: build and add descriptor ids are runtime projection ids.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testBuildAndAddDescriptorIdsAreRuntimeProjectionIds()
    {
        var fixture = fixture();
        var entry = validSpinOff(fixture).buildAndAdd().getEntry();
        var descriptorIds = name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).stream()
                        .map(descriptor -> name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport
                                        .runtimeProjectionId(entry, descriptor.getRole()))
                        .collect(Collectors.toSet());
        var runtimeUUIDs = java.util.stream.Stream.concat(
                        fixture.portfolio.getTransactions().stream()
                                        .filter(LedgerBackedTransaction.class::isInstance)
                                        .map(PortfolioTransaction::getUUID),
                        fixture.account.getTransactions().stream()
                                        .filter(LedgerBackedTransaction.class::isInstance)
                                        .map(AccountTransaction::getUUID))
                        .collect(Collectors.toSet());

        assertThat(runtimeUUIDs, is(descriptorIds));
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

        assertThat(exception.getIssue(), is(LedgerNativeEntryAssemblyIssue.NATIVE_DEFINITION_VALIDATION_FAILED));
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
     * Checks the Ledger-V6 scenario: generated descriptors target assembler owned postings.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testGeneratedDescriptorsTargetAssemblerOwnedPostings()
    {
        var fixture = fixture();
        var entry = validSpinOff(fixture).buildDetached().getEntry();
        var postingUUIDs = entry.getPostings().stream().map(LedgerPosting::getUUID).collect(Collectors.toSet());

        for (var ref : name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry))
        {
            assertTrue(postingUUIDs.contains(ref.getPrimaryPosting().getUUID()));
            assertThat(ref.getPrimaryPosting().getUnitRole(), is(LedgerPostingUnitRole.PRIMARY));

            if (ref.getPrimaryPosting().getGroupKey() != null)
                assertTrue(ref.getUnitPostings().stream()
                                .allMatch(posting -> ref.getPrimaryPosting().getGroupKey().equals(posting.getGroupKey())));
        }
    }

    private static LedgerNativeEntryAssembler.EntryBuilder validSpinOff(Fixture fixture)
    {
        return baseSpinOff(fixture) //
                        .securityLeg(sourceLeg(fixture).build()) //
                        .securityLeg(contextLeg(fixture).build()) //
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
                        .event(event());
    }

    private static LedgerNativeEntryAssembler.EntryBuilder spinOffWithRetainedAndNewLegs(Fixture fixture)
    {
        return baseSpinOff(fixture) //
                        .securityLeg(sourceLeg(fixture).build()) //
                        .securityLeg(contextLeg(fixture).build()) //
                        .securityLeg(targetLeg(fixture).projectAs(LedgerProjectionRole.DELIVERY_INBOUND).build()) //
                        .securityLeg(targetLeg(fixture).projectAs(LedgerProjectionRole.NEW_SECURITY_LEG).build()) //
                        .cashCompensation(NativeCashCompensation.builder() //
                                        .account(fixture.account) //
                                        .amount(money(5)) //
                                        .kind(CashCompensationKind.CASH_IN_LIEU) //
                                        .applied(true) //
                                        .fractionQuantity(new BigDecimal("0.5")) //
                                        .fractionTreatment(FractionTreatment.CASH_IN_LIEU) //
                                        .roundingMode(RoundingModeCode.FLOOR) //
                                        .build()) //
                        .fee(NativeFee.of(fixture.account, money(2), FeeReason.CORPORATE_ACTION_FEE)) //
                        .tax(NativeTax.withholding(fixture.account, money(1)));
    }

    private static LedgerNativeEntryAssembler.EntryBuilder repeatedSpinOff(Fixture fixture, Security secondTarget)
    {
        return baseSpinOff(fixture) //
                        .securityLeg(sourceLeg(fixture).build()) //
                        .securityLeg(contextLeg(fixture).build()) //
                        .securityLeg(targetLeg(fixture).groupKey("main").localKey("target-1").build()) //
                        .securityLeg(targetLeg(fixture) //
                                        .security(secondTarget) //
                                        .targetSecurity(secondTarget) //
                                        .shares(Values.Share.factorize(7)) //
                                        .amount(money(70)) //
                                        .groupKey("main") //
                                        .localKey("target-2") //
                                        .build()) //
                        .cashCompensation(cashCompensation(fixture, "cash-1", "cash-1", 5)) //
                        .cashCompensationMovement(cashCompensation(fixture, "cash-2", "cash-2", 7)) //
                        .fee(NativeFee.builder() //
                                        .account(fixture.account) //
                                        .amount(money(2)) //
                                        .reason(FeeReason.CORPORATE_ACTION_FEE) //
                                        .groupKey("cash-1") //
                                        .localKey("fee-1") //
                                        .build()) //
                        .tax(NativeTax.builder() //
                                        .account(fixture.account) //
                                        .amount(money(1)) //
                                        .reason(TaxReason.WITHHOLDING_TAX) //
                                        .withholdingTax(true) //
                                        .groupKey("cash-1") //
                                        .localKey("tax-1") //
                                        .build());
    }

    private static NativeCashCompensation cashCompensation(Fixture fixture, String groupKey, String localKey,
                    long amount)
    {
        return NativeCashCompensation.builder() //
                        .account(fixture.account) //
                        .amount(money(amount)) //
                        .kind(CashCompensationKind.CASH_IN_LIEU) //
                        .applied(true) //
                        .groupKey(groupKey) //
                        .localKey(localKey) //
                        .build();
    }

    private static NativeEntryMetadata metadata()
    {
        return NativeEntryMetadata.of(LocalDateTime.of(2020, 9, 28, 0, 0)) //
                        .note("Native corporate action") //
                        .source("native-entry-assembler-test");
    }

    private static NativeCorporateActionEvent event()
    {
        return NativeCorporateActionEvent.builder() //
                        .kind(CorporateActionKind.SPIN_OFF) //
                        .subtype(CorporateActionSubtype.STANDARD) //
                        .reference(CorporateActionKind.SPIN_OFF.getCode() + "-2020") //
                        .stage(EventStage.SETTLED) //
                        .effectiveDate(LocalDate.of(2020, 9, 28)) //
                        .build();
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

    private static NativeSecurityLeg.Builder contextLeg(Fixture fixture)
    {
        return NativeSecurityLeg.context() //
                        .portfolio(fixture.portfolio) //
                        .security(fixture.siemens) //
                        .shares(0L) //
                        .amount(money(0)) //
                        .groupKey("main") //
                        .localKey("context-1");
    }

    private static LedgerParameter<?> parameter(Collection<LedgerParameter<?>> parameters, LedgerParameterType type)
    {
        return parameters.stream().filter(parameter -> parameter.getType() == type).findFirst().orElseThrow();
    }

    private static DerivedProjectionDescriptor descriptor(name.abuchen.portfolio.model.ledger.LedgerEntry entry,
                    LedgerProjectionRole role)
    {
        return descriptors(entry).stream().filter(descriptor -> descriptor.getRole() == role).findFirst()
                        .orElseThrow();
    }

    private static DerivedProjectionDescriptor descriptor(name.abuchen.portfolio.model.ledger.LedgerEntry entry,
                    LedgerProjectionRole role, String semanticInstanceKey)
    {
        return descriptors(entry).stream() //
                        .filter(descriptor -> descriptor.getRole() == role) //
                        .filter(descriptor -> descriptor.getSemanticInstanceKey()
                                        .filter(semanticInstanceKey::equals).isPresent())
                        .findFirst().orElseThrow();
    }

    private static java.util.List<DerivedProjectionDescriptor> descriptors(
                    name.abuchen.portfolio.model.ledger.LedgerEntry entry)
    {
        var result = new DerivedProjectionDescriptorService().derive(entry);

        assertTrue(result.formatDiagnostics(), result.isOK());

        return result.getDescriptors();
    }

    private static java.util.Set<LedgerProjectionRole> roles(name.abuchen.portfolio.model.ledger.LedgerEntry entry)
    {
        return descriptors(entry).stream().map(DerivedProjectionDescriptor::getRole)
                        .collect(Collectors.toSet());
    }

    private static java.util.List<LedgerPosting> postings(name.abuchen.portfolio.model.ledger.LedgerEntry entry,
                    LedgerPostingType type, CorporateActionLeg leg)
    {
        return entry.getPostings().stream() //
                        .filter(posting -> posting.getType() == type) //
                        .filter(posting -> posting.getCorporateActionLeg() == leg) //
                        .toList();
    }

    private static Ledger ledger(name.abuchen.portfolio.model.ledger.LedgerEntry entry)
    {
        var ledger = new Ledger();
        ledger.addEntry(entry);
        return ledger;
    }

    private static void assertPrimarySemantics(LedgerPosting posting, LedgerPostingSemanticRole semanticRole,
                    LedgerPostingDirection direction, CorporateActionLeg leg, LedgerProjectionRole role)
    {
        assertThat(posting.getSemanticRole(), is(semanticRole));
        assertThat(posting.getDirection(), is(direction));
        assertThat(posting.getCorporateActionLeg(), is(leg));
        assertThat(posting.getUnitRole(), is(LedgerPostingUnitRole.PRIMARY));
        assertThat(posting.getGroupKey(), is(role.name()));
        assertThat(posting.getLocalKey(), is(role.name()));
    }

    private static LedgerBackedTransaction portfolioProjection(Portfolio portfolio, LedgerProjectionRole role)
    {
        return portfolio.getTransactions().stream() //
                        .filter(LedgerBackedTransaction.class::isInstance) //
                        .map(LedgerBackedTransaction.class::cast) //
                        .filter(transaction -> transaction.getLedgerProjectionDescriptor().getRole() == role) //
                        .findFirst().orElseThrow();
    }

    private static LedgerBackedTransaction accountProjection(Account account, LedgerProjectionRole role)
    {
        return account.getTransactions().stream() //
                        .filter(LedgerBackedTransaction.class::isInstance) //
                        .map(LedgerBackedTransaction.class::cast) //
                        .filter(transaction -> transaction.getLedgerProjectionDescriptor().getRole() == role) //
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
