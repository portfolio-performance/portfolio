package name.abuchen.portfolio.model.ledger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;

/**
 * Validates the structural consistency of Ledger entries.
 * This class checks Ledger facts and projection references. It does not apply business
 * repairs or guess missing transaction data.
 */
public final class LedgerStructuralValidator
{
    public enum IssueCode
    {
        LEDGER_REQUIRED,
        DUPLICATE_ENTRY_UUID,
        DUPLICATE_POSTING_UUID,
        DUPLICATE_PROJECTION_REF_UUID,
        ENTRY_UUID_REQUIRED,
        ENTRY_TYPE_REQUIRED,
        ENTRY_DATE_TIME_REQUIRED,
        POSTING_UUID_REQUIRED,
        POSTING_TYPE_REQUIRED,
        POSTING_CURRENCY_REQUIRED,
        POSTING_SECURITY_REQUIRED,
        POSTING_EXCHANGE_RATE_POSITIVE,
        DIVIDEND_SECURITY_REQUIRED,
        PROJECTION_REF_UUID_REQUIRED,
        PROJECTION_REF_ROLE_REQUIRED,
        FIXED_SHAPE_PROJECTION_ROLE_REQUIRED,
        FIXED_SHAPE_PROJECTION_ROLE_NOT_ALLOWED,
        PROJECTION_REF_ACCOUNT_REQUIRED,
        PROJECTION_REF_PORTFOLIO_REQUIRED,
        PROJECTION_REF_ACCOUNT_NOT_ALLOWED,
        PROJECTION_REF_PORTFOLIO_NOT_ALLOWED,
        TARGETING_REF_REQUIRED,
        PRIMARY_POSTING_REF_NOT_FOUND,
        POSTING_GROUP_REF_NOT_FOUND,
        PROJECTION_MEMBERSHIP_REF_NOT_FOUND,
        PROJECTION_PRIMARY_TARGET_CONFLICT,
        PROJECTION_GROUP_TARGET_CONFLICT,
        PARAMETER_TYPE_REQUIRED,
        PARAMETER_VALUE_KIND_REQUIRED,
        PARAMETER_VALUE_REQUIRED,
        PARAMETER_VALUE_KIND_MISMATCH,
        PARAMETER_CODE_NOT_ALLOWED,
        EX_DATE_VALUE_KIND_REQUIRED,
        EX_DATE_SECURITY_REQUIRED,
        SIGNED_FACT_NOT_ALLOWED
    }

    private LedgerStructuralValidator()
    {
    }

    public static ValidationResult validate(Ledger ledger)
    {
        var issues = new ArrayList<ValidationIssue>();

        if (ledger == null)
        {
            issues.add(new ValidationIssue(IssueCode.LEDGER_REQUIRED,
                            LedgerDiagnosticCode.LEDGER_STRUCT_001
                                            .message(Messages.LedgerStructuralValidatorLedgerRequired)));
            return new ValidationResult(issues);
        }

        validateEntries(ledger, issues);

        return new ValidationResult(issues);
    }

    private static void validateEntries(Ledger ledger, List<ValidationIssue> issues)
    {
        var entryUUIDCounts = new LinkedHashMap<String, Integer>();
        var postingUUIDCounts = new LinkedHashMap<String, Integer>();
        var projectionRefUUIDCounts = new LinkedHashMap<String, Integer>();

        for (var entry : ledger.getEntries())
        {
            if (isBlank(entry.getUUID()))
                issues.add(entryIssue(IssueCode.ENTRY_UUID_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_005
                                                .message(Messages.LedgerStructuralValidatorEntryUuidRequired),
                                entry));
            else
            {
                var occurrenceCount = entryUUIDCounts.merge(entry.getUUID(), 1, Integer::sum);
                if (occurrenceCount > 1)
                    issues.add(entryIssue(IssueCode.DUPLICATE_ENTRY_UUID,
                                    LedgerDiagnosticCode.LEDGER_STRUCT_006
                                                    .message(MessageFormat.format(
                                                                    Messages.LedgerStructuralValidatorDuplicateEntryUuid,
                                                                    entry.getUUID())),
                                    entry)
                                                    .withDetail("objectType", "LedgerEntry") //$NON-NLS-1$ //$NON-NLS-2$
                                                    .withDetail("duplicateUUID", entry.getUUID()) //$NON-NLS-1$
                                                    .withDetail("occurrenceCount", occurrenceCount)); //$NON-NLS-1$
            }

            if (entry.getType() == null)
                issues.add(entryIssue(IssueCode.ENTRY_TYPE_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_007
                                                .message(MessageFormat.format(
                                                                Messages.LedgerStructuralValidatorEntryTypeRequired,
                                                                entry.getUUID())),
                                entry));

            if (entry.getDateTime() == null)
                issues.add(entryIssue(IssueCode.ENTRY_DATE_TIME_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_008.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorEntryDateTimeRequired,
                                                entry.getUUID())),
                                entry));

            validateParameters(entry, null, entry.getParameters(), issues);

            var entryPostingUUIDs = validatePostings(entry, postingUUIDCounts, issues);
            validateProjectionRefs(entry, entryPostingUUIDs, projectionRefUUIDCounts, issues);
        }
    }

    private static Set<String> validatePostings(LedgerEntry entry, Map<String, Integer> ledgerPostingUUIDCounts,
                    List<ValidationIssue> issues)
    {
        var entryPostingUUIDs = new HashSet<String>();

        for (var posting : entry.getPostings())
        {
            if (isBlank(posting.getUUID()))
                issues.add(postingIssue(IssueCode.POSTING_UUID_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_009.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorPostingUuidRequired,
                                                entry.getUUID())),
                                entry, posting));
            else
            {
                entryPostingUUIDs.add(posting.getUUID());

                var occurrenceCount = ledgerPostingUUIDCounts.merge(posting.getUUID(), 1, Integer::sum);
                if (occurrenceCount > 1)
                    issues.add(postingIssue(IssueCode.DUPLICATE_POSTING_UUID,
                                    LedgerDiagnosticCode.LEDGER_STRUCT_010
                                                    .message(MessageFormat.format(
                                                                    Messages.LedgerStructuralValidatorDuplicatePostingUuid,
                                                                    posting.getUUID())),
                                    entry, posting)
                                                    .withDetail("objectType", "LedgerPosting") //$NON-NLS-1$ //$NON-NLS-2$
                                                    .withDetail("duplicateUUID", posting.getUUID()) //$NON-NLS-1$
                                                    .withDetail("occurrenceCount", occurrenceCount)); //$NON-NLS-1$
            }

            if (posting.getType() == null)
                issues.add(postingIssue(IssueCode.POSTING_TYPE_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_011.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorPostingTypeRequired,
                                                posting.getUUID())),
                                entry, posting));

            if (entry.getType() != null && !entry.getType().usesSignedTargetedProjectionFacts()
                            && (posting.getAmount() < 0 || posting.getShares() < 0))
                issues.add(postingIssue(IssueCode.SIGNED_FACT_NOT_ALLOWED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_012.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorSignedFactsNotAllowed,
                                                entry.getType())),
                                entry,
                                posting));

            validatePostingShape(entry, posting, issues);
            validateParameters(entry, posting, posting.getParameters(), issues);
        }

        validateEntryPostingShape(entry, issues);

        return entryPostingUUIDs;
    }

    private static void validateEntryPostingShape(LedgerEntry entry, List<ValidationIssue> issues)
    {
        if (entry.getType() == LedgerEntryType.DIVIDENDS
                        && entry.getPostings().stream().noneMatch(posting -> posting.getSecurity() != null))
            issues.add(entryIssue(IssueCode.DIVIDEND_SECURITY_REQUIRED,
                            LedgerDiagnosticCode.LEDGER_STRUCT_013
                                            .message(MessageFormat.format(
                                                            Messages.LedgerStructuralValidatorDividendSecurityRequired,
                                                            entry.getUUID())),
                            entry));
    }

    private static void validatePostingShape(LedgerEntry entry, LedgerPosting posting, List<ValidationIssue> issues)
    {
        if (posting.getType() == null)
            return;

        if (posting.getType().requiresCurrency() && isBlank(posting.getCurrency()))
            issues.add(postingIssue(IssueCode.POSTING_CURRENCY_REQUIRED,
                            LedgerDiagnosticCode.LEDGER_STRUCT_014.message(MessageFormat.format(
                                            Messages.LedgerStructuralValidatorPostingCurrencyRequired,
                                            posting.getUUID())),
                            entry, posting));

        if (posting.getType().requiresSecurity() && posting.getSecurity() == null)
            issues.add(postingIssue(IssueCode.POSTING_SECURITY_REQUIRED,
                            LedgerDiagnosticCode.LEDGER_STRUCT_015
                                            .message(MessageFormat.format(
                                                            Messages.LedgerStructuralValidatorPostingSecurityRequired,
                                                            posting.getUUID())),
                            entry, posting));

        if (posting.getExchangeRate() != null && posting.getExchangeRate().signum() <= 0)
            issues.add(postingIssue(IssueCode.POSTING_EXCHANGE_RATE_POSITIVE,
                            LedgerDiagnosticCode.LEDGER_FOREX_002.message(MessageFormat.format(
                                            Messages.LedgerStructuralValidatorPostingExchangeRatePositive,
                                            posting.getUUID())),
                            entry, posting));
    }

    private static void validateProjectionRefs(LedgerEntry entry, Set<String> entryPostingUUIDs,
                    Map<String, Integer> ledgerProjectionRefUUIDCounts, List<ValidationIssue> issues)
    {
        for (var projectionRef : entry.getProjectionRefs())
        {
            if (isBlank(projectionRef.getUUID()))
                issues.add(projectionIssue(IssueCode.PROJECTION_REF_UUID_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_016.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorProjectionUuidRequired,
                                                entry.getUUID())),
                                entry,
                                projectionRef));
            else
            {
                var occurrenceCount = ledgerProjectionRefUUIDCounts.merge(projectionRef.getUUID(), 1, Integer::sum);
                if (occurrenceCount > 1)
                    issues.add(projectionIssue(IssueCode.DUPLICATE_PROJECTION_REF_UUID,
                                    LedgerDiagnosticCode.LEDGER_STRUCT_017.message(MessageFormat.format(
                                                    Messages.LedgerStructuralValidatorDuplicateProjectionUuid,
                                                    projectionRef.getUUID())),
                                    entry,
                                    projectionRef).withDetail("objectType", "LedgerProjectionRef") //$NON-NLS-1$ //$NON-NLS-2$
                                                    .withDetail("duplicateUUID", projectionRef.getUUID()) //$NON-NLS-1$
                                                    .withDetail("occurrenceCount", occurrenceCount)); //$NON-NLS-1$
            }

            if (projectionRef.getRole() == null)
            {
                issues.add(projectionIssue(IssueCode.PROJECTION_REF_ROLE_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_018.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorProjectionRoleRequired,
                                                projectionRef.getUUID())),
                                entry,
                                projectionRef));
                continue;
            }

            validateProjectionOwner(entry, projectionRef, issues);
            validateProjectionMemberships(entry, projectionRef, entryPostingUUIDs, issues);
            validatePrimaryPostingRef(entry, projectionRef, entryPostingUUIDs, issues);
            validateTargeting(entry, projectionRef, entryPostingUUIDs, issues);
        }

        validateFixedShapeProjectionRoles(entry, issues);
    }

    private static void validateFixedShapeProjectionRoles(LedgerEntry entry, List<ValidationIssue> issues)
    {
        if (entry.getType() == null || !entry.getType().isLegacyFixedShape())
            return;

        var expectedRoles = expectedProjectionRoles(entry.getType());
        var roleCounts = new EnumMap<LedgerProjectionRole, Integer>(LedgerProjectionRole.class);

        for (var projectionRef : entry.getProjectionRefs())
        {
            var role = projectionRef.getRole();

            if (role == null)
                continue;

            roleCounts.merge(role, 1, Integer::sum);
            if (!expectedRoles.contains(role))
                issues.add(projectionIssue(IssueCode.FIXED_SHAPE_PROJECTION_ROLE_NOT_ALLOWED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_019.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorProjectionRoleNotAllowed,
                                                role, entry.getType())),
                                entry,
                                projectionRef));
        }

        for (var expectedRole : expectedRoles)
        {
            if (roleCounts.getOrDefault(expectedRole, 0) != 1)
                issues.add(entryIssue(IssueCode.FIXED_SHAPE_PROJECTION_ROLE_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_020.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorProjectionRoleRequiredForType,
                                                entry.getType(), expectedRole)),
                                entry).withDetail("expectedProjectionRole", expectedRole)); //$NON-NLS-1$
        }
    }

    private static Set<LedgerProjectionRole> expectedProjectionRoles(LedgerEntryType entryType)
    {
        return switch (entryType)
        {
            case DEPOSIT, REMOVAL, INTEREST, INTEREST_CHARGE, FEES, FEES_REFUND, TAXES, TAX_REFUND, DIVIDENDS ->
                EnumSet.of(LedgerProjectionRole.ACCOUNT);
            case BUY, SELL -> EnumSet.of(LedgerProjectionRole.ACCOUNT, LedgerProjectionRole.PORTFOLIO);
            case CASH_TRANSFER -> EnumSet.of(LedgerProjectionRole.SOURCE_ACCOUNT, LedgerProjectionRole.TARGET_ACCOUNT);
            case SECURITY_TRANSFER ->
                EnumSet.of(LedgerProjectionRole.SOURCE_PORTFOLIO, LedgerProjectionRole.TARGET_PORTFOLIO);
            case DELIVERY_INBOUND -> EnumSet.of(LedgerProjectionRole.DELIVERY_INBOUND);
            case DELIVERY_OUTBOUND -> EnumSet.of(LedgerProjectionRole.DELIVERY_OUTBOUND);
            default -> EnumSet.noneOf(LedgerProjectionRole.class);
        };
    }

    private static void validateProjectionOwner(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    List<ValidationIssue> issues)
    {
        if (requiresAccount(projectionRef.getRole()))
        {
            if (projectionRef.getAccount() == null)
                issues.add(projectionIssue(IssueCode.PROJECTION_REF_ACCOUNT_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_021
                                                .message(MessageFormat.format(
                                                                Messages.LedgerStructuralValidatorProjectionAccountRequired,
                                                                projectionRef.getRole())),
                                entry,
                                projectionRef));

            if (projectionRef.getPortfolio() != null)
                issues.add(projectionIssue(IssueCode.PROJECTION_REF_PORTFOLIO_NOT_ALLOWED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_022.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorAccountProjectionPortfolioNotAllowed,
                                                projectionRef.getRole())),
                                entry, projectionRef));
        }

        if (requiresPortfolio(projectionRef.getRole()))
        {
            if (projectionRef.getPortfolio() == null)
                issues.add(projectionIssue(IssueCode.PROJECTION_REF_PORTFOLIO_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_023
                                                .message(MessageFormat.format(
                                                                Messages.LedgerStructuralValidatorProjectionPortfolioRequired,
                                                                projectionRef.getRole())),
                                entry,
                                projectionRef));

            if (projectionRef.getAccount() != null)
                issues.add(projectionIssue(IssueCode.PROJECTION_REF_ACCOUNT_NOT_ALLOWED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_024.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorPortfolioProjectionAccountNotAllowed,
                                                projectionRef.getRole())),
                                entry, projectionRef));
        }
    }

    private static void validatePrimaryPostingRef(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    Set<String> entryPostingUUIDs, List<ValidationIssue> issues)
    {
        if (!isBlank(projectionRef.getPrimaryPostingUUID())
                        && !entryPostingUUIDs.contains(projectionRef.getPrimaryPostingUUID()))
            issues.add(projectionIssue(IssueCode.PRIMARY_POSTING_REF_NOT_FOUND,
                            LedgerDiagnosticCode.LEDGER_STRUCT_002
                                            .message(MessageFormat.format(
                                                            Messages.LedgerStructuralValidatorPrimaryPostingRefNotFound,
                                                            projectionRef.getPrimaryPostingUUID())),
                            entry, projectionRef));
    }

    private static void validateProjectionMemberships(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    Set<String> entryPostingUUIDs, List<ValidationIssue> issues)
    {
        for (var membership : projectionRef.getMemberships())
        {
            var postingUUID = membership.getPostingUUID();

            if (!entryPostingUUIDs.contains(postingUUID))
                issues.add(projectionIssue(IssueCode.PROJECTION_MEMBERSHIP_REF_NOT_FOUND,
                                LedgerDiagnosticCode.LEDGER_STRUCT_003
                                                .message(MessageFormat.format(
                                                                Messages.LedgerStructuralValidatorProjectionMembershipRefNotFound,
                                                                postingUUID)),
                                entry, projectionRef).withDetail("membershipRole", membership.getRole()) //$NON-NLS-1$
                                                .withDetail("membershipPostingUUID", postingUUID)); //$NON-NLS-1$
        }

        projectionRef.getPrimaryMembership().ifPresent(membership -> {
            var primaryPostingUUID = projectionRef.getPrimaryPostingUUID();

            if (!isBlank(primaryPostingUUID) && !primaryPostingUUID.equals(membership.getPostingUUID()))
                issues.add(projectionIssue(IssueCode.PROJECTION_PRIMARY_TARGET_CONFLICT,
                                LedgerDiagnosticCode.LEDGER_STRUCT_025.message(
                                                Messages.LedgerStructuralValidatorProjectionPrimaryTargetConflict),
                                entry, projectionRef).withDetail("membershipRole", membership.getRole()) //$NON-NLS-1$
                                                .withDetail("membershipPostingUUID", membership.getPostingUUID())); //$NON-NLS-1$
        });

        projectionRef.getMembershipsByRole(ProjectionMembershipRole.GROUP_ANCHOR).stream().findFirst()
                        .ifPresent(membership -> {
                            var postingGroupUUID = projectionRef.getPostingGroupUUID();

                            if (!isBlank(postingGroupUUID) && !postingGroupUUID.equals(membership.getPostingUUID()))
                                issues.add(projectionIssue(IssueCode.PROJECTION_GROUP_TARGET_CONFLICT,
                                                LedgerDiagnosticCode.LEDGER_STRUCT_026.message(
                                                                Messages.LedgerStructuralValidatorProjectionGroupTargetConflict),
                                                entry, projectionRef).withDetail("membershipRole", membership.getRole()) //$NON-NLS-1$
                                                                .withDetail("membershipPostingUUID", //$NON-NLS-1$
                                                                                membership.getPostingUUID()));
                        });
    }

    private static boolean requiresAccount(LedgerProjectionRole role)
    {
        return switch (role)
        {
            case ACCOUNT, SOURCE_ACCOUNT, TARGET_ACCOUNT, CASH_COMPENSATION -> true;
            default -> false;
        };
    }

    private static boolean requiresPortfolio(LedgerProjectionRole role)
    {
        return switch (role)
        {
            case PORTFOLIO, SOURCE_PORTFOLIO, TARGET_PORTFOLIO, DELIVERY, DELIVERY_INBOUND, DELIVERY_OUTBOUND,
                            OLD_SECURITY_LEG, NEW_SECURITY_LEG -> true;
            default -> false;
        };
    }

    private static void validateTargeting(LedgerEntry entry, LedgerProjectionRef projectionRef,
                    Set<String> entryPostingUUIDs, List<ValidationIssue> issues)
    {
        if (entry.getType() == null || !entry.getType().requiresTargetedProjectionRefs())
            return;

        if (projectionRef.getPrimaryMembership().isEmpty() && isBlank(projectionRef.getPrimaryPostingUUID()))
        {
            issues.add(projectionIssue(IssueCode.TARGETING_REF_REQUIRED,
                            LedgerDiagnosticCode.LEDGER_STRUCT_027
                                            .message(Messages.LedgerStructuralValidatorTargetingRefRequired),
                            entry, projectionRef));
            return;
        }

        if (!isBlank(projectionRef.getPostingGroupUUID())
                        && !entryPostingUUIDs.contains(projectionRef.getPostingGroupUUID()))
            issues.add(projectionIssue(IssueCode.POSTING_GROUP_REF_NOT_FOUND,
                            LedgerDiagnosticCode.LEDGER_STRUCT_004
                                            .message(MessageFormat.format(
                                                            Messages.LedgerStructuralValidatorPostingGroupRefNotFound,
                                                            projectionRef.getPostingGroupUUID())),
                            entry, projectionRef));
    }

    private static void validateParameters(LedgerEntry entry, LedgerPosting posting,
                    List<LedgerParameter<?>> parameters, List<ValidationIssue> issues)
    {
        var owner = parameterOwnerDescription(entry, posting);

        for (var parameter : parameters)
        {
            if (parameter.getType() == null)
                issues.add(parameterIssue(IssueCode.PARAMETER_TYPE_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_028
                                                .message(MessageFormat.format(
                                                                Messages.LedgerStructuralValidatorParameterTypeRequired,
                                                                owner)),
                                entry, posting,
                                parameter));

            if (parameter.getValueKind() == null)
            {
                issues.add(parameterIssue(IssueCode.PARAMETER_VALUE_KIND_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_029.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorParameterValueKindRequired,
                                                owner)),
                                entry,
                                posting, parameter));
                continue;
            }

            if (parameter.getValue() == null)
                issues.add(parameterIssue(IssueCode.PARAMETER_VALUE_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_030
                                                .message(MessageFormat.format(
                                                                Messages.LedgerStructuralValidatorParameterValueRequired,
                                                                owner)),
                                entry, posting,
                                parameter));

            if (parameter.getType() != null && !parameter.getType().supportsValueKind(parameter.getValueKind()))
                issues.add(parameterValueKindIssue(entry, posting, parameter));

            if (posting != null && parameter.getType() == LedgerParameterType.EX_DATE
                            && posting.getSecurity() == null)
                issues.add(parameterIssue(IssueCode.EX_DATE_SECURITY_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_031
                                                .message(MessageFormat.format(
                                                                Messages.LedgerStructuralValidatorExDateSecurityRequired,
                                                                posting.getUUID())),
                                entry, posting,
                                parameter));

            if (parameter.getValue() != null && !isCompatible(parameter))
                issues.add(parameterIssue(IssueCode.PARAMETER_VALUE_KIND_MISMATCH,
                                LedgerDiagnosticCode.LEDGER_STRUCT_032.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorParameterValueKindMismatch,
                                                parameter.getValueKind())),
                                entry, posting, parameter));

            if (hasUnsupportedCode(parameter))
                issues.add(parameterIssue(IssueCode.PARAMETER_CODE_NOT_ALLOWED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_033.message(MessageFormat.format(
                                                Messages.LedgerStructuralValidatorParameterCodeNotAllowed,
                                                parameter.getType())),
                                entry, posting,
                                parameter).withDetail("codeDomain", parameter.getType().getCodeDomain()) //$NON-NLS-1$
                                                .withDetail("allowedCodes", //$NON-NLS-1$
                                                                parameter.getType().getCodeDomain().getAllowedCodes()));
        }
    }

    private static String parameterOwnerDescription(LedgerEntry entry, LedgerPosting posting)
    {
        if (posting != null)
            return "posting " + posting.getUUID(); //$NON-NLS-1$

        return "entry " + entry.getUUID(); //$NON-NLS-1$
    }

    private static boolean isCompatible(LedgerParameter<?> parameter)
    {
        return parameter.getValueKind().supportsValue(parameter.getValue());
    }

    private static boolean hasUnsupportedCode(LedgerParameter<?> parameter)
    {
        return parameter.getType() != null && parameter.getType().hasCodeDomain()
                        && parameter.getType().supportsValueKind(parameter.getValueKind()) && isCompatible(parameter)
                        && !parameter.getType().supportsCode((String) parameter.getValue());
    }

    private static boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }

    private static ValidationIssue entryIssue(IssueCode code, String message, LedgerEntry entry)
    {
        return new ValidationIssue(code, message).withEntry(entry);
    }

    private static ValidationIssue postingIssue(IssueCode code, String message, LedgerEntry entry,
                    LedgerPosting posting)
    {
        return entryIssue(code, message, entry).withPosting(posting);
    }

    private static ValidationIssue projectionIssue(IssueCode code, String message, LedgerEntry entry,
                    LedgerProjectionRef projectionRef)
    {
        return entryIssue(code, message, entry).withProjection(projectionRef);
    }

    private static ValidationIssue parameterIssue(IssueCode code, String message, LedgerEntry entry,
                    LedgerPosting posting, LedgerParameter<?> parameter)
    {
        return postingIssue(code, message, entry, posting).withParameter(parameter);
    }

    private static ValidationIssue parameterValueKindIssue(LedgerEntry entry, LedgerPosting posting,
                    LedgerParameter<?> parameter)
    {
        var type = parameter.getType();
        var code = type == LedgerParameterType.EX_DATE ? IssueCode.EX_DATE_VALUE_KIND_REQUIRED
                        : IssueCode.PARAMETER_VALUE_KIND_MISMATCH;

        return parameterIssue(code,
                        LedgerDiagnosticCode.LEDGER_STRUCT_034
                                        .message(MessageFormat.format(
                                                        Messages.LedgerStructuralValidatorParameterMustUseValueKind,
                                                        type, type.getExpectedValueKind())),
                        entry, posting, parameter)
                        .withDetail("expectedValueKind", type.getExpectedValueKind()) //$NON-NLS-1$
                        .withDetail("actualValueKind", parameter.getValueKind()); //$NON-NLS-1$
    }

    private static String ownerSummary(Account account)
    {
        if (account == null)
            return null;

        return summary(account.getName(), account.getUUID());
    }

    private static String ownerSummary(Portfolio portfolio)
    {
        if (portfolio == null)
            return null;

        return summary(portfolio.getName(), portfolio.getUUID());
    }

    private static String securitySummary(Security security)
    {
        if (security == null)
            return null;

        return summary(security.getName(), security.getUUID());
    }

    private static String summary(String name, String uuid)
    {
        var displayName = isBlank(name) ? "<missing>" : name; //$NON-NLS-1$
        var displayUUID = isBlank(uuid) ? "<missing>" : uuid; //$NON-NLS-1$

        return displayName + " (" + displayUUID + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static String valueSummary(Object value)
    {
        if (value == null)
            return null;

        if (value instanceof Account account)
            return ownerSummary(account);

        if (value instanceof Portfolio portfolio)
            return ownerSummary(portfolio);

        if (value instanceof Security security)
            return securitySummary(security);

        var summary = String.valueOf(value);

        return summary.length() > 120 ? summary.substring(0, 117) + "..." : summary; //$NON-NLS-1$
    }

    public static final class ValidationResult
    {
        private final List<ValidationIssue> issues;

        private ValidationResult(List<ValidationIssue> issues)
        {
            this.issues = List.copyOf(issues);
        }

        public boolean isOK()
        {
            return issues.isEmpty();
        }

        public List<ValidationIssue> getIssues()
        {
            return Collections.unmodifiableList(issues);
        }

        public boolean hasIssue(IssueCode code)
        {
            return issues.stream().anyMatch(issue -> issue.getCode() == code);
        }

        public String format()
        {
            if (issues.isEmpty())
                return "OK"; //$NON-NLS-1$

            var builder = new StringBuilder();

            for (var issue : issues)
            {
                if (!builder.isEmpty())
                    builder.append("\n\n"); //$NON-NLS-1$

                builder.append(issue.format());
            }

            return builder.toString();
        }

        @Override
        public String toString()
        {
            return format();
        }
    }

    public static final class ValidationIssue
    {
        private final IssueCode code;
        private final String message;
        private final Map<String, String> details = new LinkedHashMap<>();

        private ValidationIssue(IssueCode code, String message)
        {
            this.code = code;
            this.message = message;
        }

        public IssueCode getCode()
        {
            return code;
        }

        public String getMessage()
        {
            return message;
        }

        public Map<String, String> getDetails()
        {
            return Collections.unmodifiableMap(details);
        }

        public String format()
        {
            if (details.isEmpty())
                return "[" + code + "] " + message; //$NON-NLS-1$ //$NON-NLS-2$

            var builder = new StringBuilder();

            builder.append("[").append(code).append("] ").append(message); //$NON-NLS-1$ //$NON-NLS-2$
            appendGroup(builder, "Entry", //$NON-NLS-1$
                            detail("UUID", "entryUUID"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("Type", "entryType"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("DateTime", "entryDateTime")); //$NON-NLS-1$ //$NON-NLS-2$
            appendGroup(builder, "Posting", //$NON-NLS-1$
                            detail("UUID", "postingUUID"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("Type", "postingType"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("Amount", "amount"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("Currency", "currency"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("Shares", "shares"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("ExchangeRate", "exchangeRate"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("Security", "security"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("Account", "postingAccount"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("Portfolio", "postingPortfolio")); //$NON-NLS-1$ //$NON-NLS-2$
            appendGroup(builder, "Projection", //$NON-NLS-1$
                            detail("UUID", "projectionUUID"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("Role", "projectionRole"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("ExpectedRole", "expectedProjectionRole"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("Account", "projectionAccount"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("Portfolio", "projectionPortfolio"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("PrimaryPostingUUID", "primaryPostingUUID"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("PostingGroupUUID", "postingGroupUUID"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("MembershipRole", "membershipRole"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("MembershipPostingUUID", "membershipPostingUUID")); //$NON-NLS-1$ //$NON-NLS-2$
            appendGroup(builder, "Parameter", //$NON-NLS-1$
                            detail("Type", "parameterType"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("ExpectedValueKind", "expectedValueKind"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("ValueKind", "actualValueKind"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("ValueType", "actualValueType"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("Value", "actualValue"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("CodeDomain", "codeDomain"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("AllowedCodes", "allowedCodes")); //$NON-NLS-1$ //$NON-NLS-2$
            appendGroup(builder, "Duplicate", //$NON-NLS-1$
                            detail("ObjectType", "objectType"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("DuplicateUUID", "duplicateUUID"), //$NON-NLS-1$ //$NON-NLS-2$
                            detail("OccurrenceCount", "occurrenceCount")); //$NON-NLS-1$ //$NON-NLS-2$

            return builder.toString();
        }

        @Override
        public String toString()
        {
            return format();
        }

        private ValidationIssue withEntry(LedgerEntry entry)
        {
            if (entry == null)
                return this;

            return withDetail("entryUUID", entry.getUUID()) //$NON-NLS-1$
                            .withDetail("entryType", entry.getType()) //$NON-NLS-1$
                            .withDetail("entryDateTime", entry.getDateTime()); //$NON-NLS-1$
        }

        private ValidationIssue withPosting(LedgerPosting posting)
        {
            if (posting == null)
                return this;

            return withDetail("postingUUID", posting.getUUID()) //$NON-NLS-1$
                            .withDetail("postingType", posting.getType()) //$NON-NLS-1$
                            .withDetail("amount", posting.getAmount()) //$NON-NLS-1$
                            .withDetail("currency", posting.getCurrency()) //$NON-NLS-1$
                            .withDetail("shares", posting.getShares()) //$NON-NLS-1$
                            .withDetail("exchangeRate", posting.getExchangeRate()) //$NON-NLS-1$
                            .withDetail("security", securitySummary(posting.getSecurity())) //$NON-NLS-1$
                            .withDetail("postingAccount", ownerSummary(posting.getAccount())) //$NON-NLS-1$
                            .withDetail("postingPortfolio", ownerSummary(posting.getPortfolio())); //$NON-NLS-1$
        }

        private ValidationIssue withProjection(LedgerProjectionRef projectionRef)
        {
            if (projectionRef == null)
                return this;

            return withDetail("projectionUUID", projectionRef.getUUID()) //$NON-NLS-1$
                            .withDetail("projectionRole", projectionRef.getRole()) //$NON-NLS-1$
                            .withDetail("projectionAccount", ownerSummary(projectionRef.getAccount())) //$NON-NLS-1$
                            .withDetail("projectionPortfolio", ownerSummary(projectionRef.getPortfolio())) //$NON-NLS-1$
                            .withDetail("primaryPostingUUID", projectionRef.getPrimaryPostingUUID()) //$NON-NLS-1$
                            .withDetail("postingGroupUUID", projectionRef.getPostingGroupUUID()); //$NON-NLS-1$
        }

        private ValidationIssue withParameter(LedgerParameter<?> parameter)
        {
            if (parameter == null)
                return this;

            var value = parameter.getValue();

            return withDetail("parameterType", parameter.getType()) //$NON-NLS-1$
                            .withDetail("actualValueKind", parameter.getValueKind()) //$NON-NLS-1$
                            .withDetail("actualValueType", value == null ? null : value.getClass().getSimpleName()) //$NON-NLS-1$
                            .withDetail("actualValue", valueSummary(value)); //$NON-NLS-1$
        }

        private ValidationIssue withDetail(String key, Object value)
        {
            details.put(key, detailValue(value));

            return this;
        }

        private Detail detail(String label, String key)
        {
            return new Detail(label, key);
        }

        private void appendGroup(StringBuilder builder, String group, Detail... groupDetails)
        {
            var hasDetails = false;

            for (var groupDetail : groupDetails)
            {
                if (details.containsKey(groupDetail.key))
                {
                    hasDetails = true;
                    break;
                }
            }

            if (!hasDetails)
                return;

            builder.append("\n\n  ").append(group).append(":"); //$NON-NLS-1$ //$NON-NLS-2$

            for (var groupDetail : groupDetails)
            {
                if (details.containsKey(groupDetail.key))
                    builder.append("\n    ").append(groupDetail.label).append(": ") //$NON-NLS-1$ //$NON-NLS-2$
                                    .append(details.get(groupDetail.key));
            }
        }

        private String detailValue(Object value)
        {
            if (value == null)
                return "<missing>"; //$NON-NLS-1$

            var string = String.valueOf(value);

            return string.isBlank() ? "<missing>" : string; //$NON-NLS-1$
        }
    }

    private record Detail(String label, String key)
    {
    }
}
