package name.abuchen.portfolio.model.ledger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;

/**
 * Validates the structural consistency of Ledger entries.
 * This class checks Ledger facts and semantic projection descriptors. It does not apply business
 * repairs or guess missing transaction data.
 */
public final class LedgerStructuralValidator
{
    public enum IssueCode
    {
        LEDGER_REQUIRED,
        ENTRY_TYPE_REQUIRED,
        ENTRY_DATE_TIME_REQUIRED,
        POSTING_TYPE_REQUIRED,
        POSTING_CURRENCY_REQUIRED,
        POSTING_SECURITY_REQUIRED,
        POSTING_EXCHANGE_RATE_POSITIVE,
        DIVIDEND_SECURITY_REQUIRED,
        PARAMETER_TYPE_REQUIRED,
        PARAMETER_VALUE_KIND_REQUIRED,
        PARAMETER_VALUE_REQUIRED,
        PARAMETER_VALUE_KIND_MISMATCH,
        PARAMETER_CODE_NOT_ALLOWED,
        EX_DATE_VALUE_KIND_REQUIRED,
        EX_DATE_SECURITY_REQUIRED,
        SIGNED_FACT_NOT_ALLOWED,
        SEMANTIC_PRIMARY_REQUIRED,
        SEMANTIC_PRIMARY_AMBIGUOUS,
        SEMANTIC_SOURCE_REQUIRED,
        SEMANTIC_TARGET_REQUIRED,
        SEMANTIC_SOURCE_AMBIGUOUS,
        SEMANTIC_TARGET_AMBIGUOUS,
        SEMANTIC_OWNER_REQUIRED,
        SEMANTIC_UNIT_GROUP_REQUIRED,
        SEMANTIC_UNIT_GROUP_AMBIGUOUS,
        SEMANTIC_LOCAL_KEY_REQUIRED,
        CORPORATE_ACTION_LEG_REQUIRED,
        CORPORATE_ACTION_LEG_AMBIGUOUS
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
        for (var entry : ledger.getEntries())
        {
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

            validatePostings(entry, issues);
        }
    }

    private static void validatePostings(LedgerEntry entry, List<ValidationIssue> issues)
    {
        for (var posting : entry.getPostings())
        {
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
        validateSemanticShape(entry, issues);
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

    private static void validateSemanticShape(LedgerEntry entry, List<ValidationIssue> issues)
    {
        if (entry.getType() == null)
            return;

        switch (entry.getType())
        {
            case DEPOSIT, REMOVAL, INTEREST, INTEREST_CHARGE, DIVIDENDS ->
                requirePrimary(entry, LedgerProjectionRole.ACCOUNT, LedgerPostingSemanticRole.CASH,
                                LedgerPostingDirection.NEUTRAL, OwnerKind.ACCOUNT, null, false, issues);
            case FEES, FEES_REFUND -> requirePrimary(entry, LedgerProjectionRole.ACCOUNT, LedgerPostingSemanticRole.FEE,
                            LedgerPostingDirection.NEUTRAL, OwnerKind.ACCOUNT, null, false, issues);
            case TAXES, TAX_REFUND -> requirePrimary(entry, LedgerProjectionRole.ACCOUNT, LedgerPostingSemanticRole.TAX,
                            LedgerPostingDirection.NEUTRAL, OwnerKind.ACCOUNT, null, false, issues);
            case BUY -> {
                requirePrimary(entry, LedgerProjectionRole.ACCOUNT, LedgerPostingSemanticRole.CASH,
                                LedgerPostingDirection.OUTBOUND, OwnerKind.ACCOUNT, null, false, issues);
                requirePrimary(entry, LedgerProjectionRole.PORTFOLIO, LedgerPostingSemanticRole.SECURITY,
                                LedgerPostingDirection.INBOUND, OwnerKind.PORTFOLIO, null, false, issues);
            }
            case SELL -> {
                requirePrimary(entry, LedgerProjectionRole.ACCOUNT, LedgerPostingSemanticRole.CASH,
                                LedgerPostingDirection.INBOUND, OwnerKind.ACCOUNT, null, false, issues);
                requirePrimary(entry, LedgerProjectionRole.PORTFOLIO, LedgerPostingSemanticRole.SECURITY,
                                LedgerPostingDirection.OUTBOUND, OwnerKind.PORTFOLIO, null, false, issues);
            }
            case DELIVERY_INBOUND -> requirePrimary(entry, LedgerProjectionRole.DELIVERY_INBOUND,
                            LedgerPostingSemanticRole.SECURITY, LedgerPostingDirection.INBOUND, OwnerKind.PORTFOLIO,
                            null, false, issues);
            case DELIVERY_OUTBOUND -> requirePrimary(entry, LedgerProjectionRole.DELIVERY_OUTBOUND,
                            LedgerPostingSemanticRole.SECURITY, LedgerPostingDirection.OUTBOUND, OwnerKind.PORTFOLIO,
                            null, false, issues);
            case CASH_TRANSFER -> {
                requirePrimary(entry, LedgerProjectionRole.SOURCE_ACCOUNT, LedgerPostingSemanticRole.CASH,
                                LedgerPostingDirection.OUTBOUND, OwnerKind.ACCOUNT, null, false, issues);
                requirePrimary(entry, LedgerProjectionRole.TARGET_ACCOUNT, LedgerPostingSemanticRole.CASH,
                                LedgerPostingDirection.INBOUND, OwnerKind.ACCOUNT, null, false, issues);
            }
            case SECURITY_TRANSFER -> {
                requirePrimary(entry, LedgerProjectionRole.SOURCE_PORTFOLIO, LedgerPostingSemanticRole.SECURITY,
                                LedgerPostingDirection.OUTBOUND, OwnerKind.PORTFOLIO, null, false, issues);
                requirePrimary(entry, LedgerProjectionRole.TARGET_PORTFOLIO, LedgerPostingSemanticRole.SECURITY,
                                LedgerPostingDirection.INBOUND, OwnerKind.PORTFOLIO, null, false, issues);
            }
            case CORPORATE_ACTION -> validateCorporateActionSemanticShape(entry, issues);
            default -> {
                // No semantic shape rule.
            }
        }

        validateUnitGrouping(entry, issues);
    }

    private static void validateCorporateActionSemanticShape(LedgerEntry entry, List<ValidationIssue> issues)
    {
        var kind = CorporateActionKind.fromEntry(entry);

        if (kind.filter(CorporateActionKind.SPIN_OFF::equals).isEmpty())
            return;

        requireRepeatableCorporatePrimary(entry, LedgerProjectionRole.OLD_SECURITY_LEG,
                        LedgerPostingSemanticRole.SECURITY, CorporateActionLeg.SOURCE_SECURITY,
                        LedgerPostingDirection.OUTBOUND, true, issues);
        requireRepeatableCorporatePrimary(entry, LedgerProjectionRole.NEW_SECURITY_LEG,
                        LedgerPostingSemanticRole.SECURITY, CorporateActionLeg.TARGET_SECURITY,
                        LedgerPostingDirection.INBOUND, true, issues, LedgerProjectionRole.DELIVERY_INBOUND);
        requireRepeatableCorporatePrimary(entry, LedgerProjectionRole.CASH_COMPENSATION,
                        LedgerPostingSemanticRole.CASH_COMPENSATION, CorporateActionLeg.CASH_COMPENSATION,
                        LedgerPostingDirection.NEUTRAL, true, issues);
    }

    private static void requireRepeatableCorporatePrimary(LedgerEntry entry, LedgerProjectionRole role,
                    LedgerPostingSemanticRole semanticRole, CorporateActionLeg leg, LedgerPostingDirection direction,
                    boolean optional, List<ValidationIssue> issues, LedgerProjectionRole... excludedLocalKeys)
    {
        var matches = entry.getPostings().stream() //
                        .filter(posting -> matchesPrimary(posting, semanticRole, direction, leg)) //
                        .filter(posting -> !hasExcludedLocalKey(posting, excludedLocalKeys)) //
                        .toList();

        validateRepeatablePrimaryMatches(entry, role, OwnerKind.PORTFOLIO_OR_ACCOUNT, optional, matches, issues);
    }

    private static boolean hasExcludedLocalKey(LedgerPosting posting, LedgerProjectionRole... excludedLocalKeys)
    {
        for (var role : excludedLocalKeys)
            if (role.name().equals(posting.getLocalKey()))
                return true;

        return false;
    }

    private static void requirePrimary(LedgerEntry entry, LedgerProjectionRole role,
                    LedgerPostingSemanticRole semanticRole, LedgerPostingDirection direction, OwnerKind ownerKind,
                    CorporateActionLeg leg, boolean optional, List<ValidationIssue> issues)
    {
        var matches = entry.getPostings().stream() //
                        .filter(posting -> matchesPrimary(posting, semanticRole, direction, leg)) //
                        .toList();

        validatePrimaryMatches(entry, role, ownerKind, optional, matches, issues);
    }

    private static void validatePrimaryMatches(LedgerEntry entry, LedgerProjectionRole role, OwnerKind ownerKind,
                    boolean optional, List<LedgerPosting> matches, List<ValidationIssue> issues)
    {
        if (matches.isEmpty())
        {
            if (!optional)
                issues.add(entryIssue(missingIssueCode(role),
                                LedgerDiagnosticCode.LEDGER_STRUCT_016
                                                .message("Required semantic primary posting is missing for " + role), //$NON-NLS-1$
                                entry).withDetail("projectionRole", role)); //$NON-NLS-1$
            return;
        }

        if (matches.size() > 1)
            issues.add(entryIssue(ambiguousIssueCode(role),
                            LedgerDiagnosticCode.LEDGER_STRUCT_017
                                            .message("Semantic primary posting is ambiguous for " + role), //$NON-NLS-1$
                            entry).withDetail("projectionRole", role) //$NON-NLS-1$
                                            .withDetail("actualCount", matches.size())); //$NON-NLS-1$

        for (var posting : matches)
            validateOwner(entry, role, posting, ownerKind, issues);
    }

    private static void validateRepeatablePrimaryMatches(LedgerEntry entry, LedgerProjectionRole role,
                    OwnerKind ownerKind, boolean optional, List<LedgerPosting> matches, List<ValidationIssue> issues)
    {
        if (matches.isEmpty())
        {
            if (!optional)
                issues.add(entryIssue(missingIssueCode(role),
                                LedgerDiagnosticCode.LEDGER_STRUCT_016
                                                .message("Required semantic primary posting is missing for " + role), //$NON-NLS-1$
                                entry).withDetail("projectionRole", role)); //$NON-NLS-1$
            return;
        }

        if (matches.size() > 1)
            validateRepeatablePrimaryLocalKeys(entry, role, matches, issues);

        for (var posting : matches)
            validateOwner(entry, role, posting, ownerKind, issues);
    }

    private static void validateRepeatablePrimaryLocalKeys(LedgerEntry entry, LedgerProjectionRole role,
                    List<LedgerPosting> matches, List<ValidationIssue> issues)
    {
        var localKeys = new HashSet<String>();

        for (var posting : matches)
        {
            if (isBlank(posting.getLocalKey()))
            {
                issues.add(postingIssue(IssueCode.SEMANTIC_LOCAL_KEY_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_027
                                                .message("Repeated semantic primary posting requires a local key for " //$NON-NLS-1$
                                                                + role),
                                entry, posting).withDetail("projectionRole", role)); //$NON-NLS-1$
            }
            else if (!localKeys.add(posting.getLocalKey()))
            {
                issues.add(entryIssue(ambiguousIssueCode(role),
                                LedgerDiagnosticCode.LEDGER_STRUCT_017
                                                .message("Repeated semantic primary posting local key is duplicated for " //$NON-NLS-1$
                                                                + role),
                                entry).withDetail("projectionRole", role) //$NON-NLS-1$
                                                .withDetail("localKey", posting.getLocalKey())); //$NON-NLS-1$
            }
        }
    }

    private static IssueCode missingIssueCode(LedgerProjectionRole role)
    {
        return switch (role)
        {
            case SOURCE_ACCOUNT, SOURCE_PORTFOLIO, OLD_SECURITY_LEG -> IssueCode.SEMANTIC_SOURCE_REQUIRED;
            case TARGET_ACCOUNT, TARGET_PORTFOLIO, NEW_SECURITY_LEG -> IssueCode.SEMANTIC_TARGET_REQUIRED;
            default -> IssueCode.SEMANTIC_PRIMARY_REQUIRED;
        };
    }

    private static IssueCode ambiguousIssueCode(LedgerProjectionRole role)
    {
        return switch (role)
        {
            case SOURCE_ACCOUNT, SOURCE_PORTFOLIO, OLD_SECURITY_LEG -> IssueCode.SEMANTIC_SOURCE_AMBIGUOUS;
            case TARGET_ACCOUNT, TARGET_PORTFOLIO, NEW_SECURITY_LEG -> IssueCode.SEMANTIC_TARGET_AMBIGUOUS;
            default -> IssueCode.SEMANTIC_PRIMARY_AMBIGUOUS;
        };
    }

    private static boolean matchesPrimary(LedgerPosting posting, LedgerPostingSemanticRole semanticRole,
                    LedgerPostingDirection direction, CorporateActionLeg leg)
    {
        return posting.getUnitRole() == LedgerPostingUnitRole.PRIMARY && posting.getSemanticRole() == semanticRole
                        && posting.getDirection() == direction
                        && (leg == null || posting.getCorporateActionLeg() == leg);
    }

    private static void validateOwner(LedgerEntry entry, LedgerProjectionRole role, LedgerPosting posting,
                    OwnerKind ownerKind, List<ValidationIssue> issues)
    {
        var ownerPresent = switch (ownerKind)
        {
            case ACCOUNT -> posting.getAccount() != null;
            case PORTFOLIO -> posting.getPortfolio() != null;
            case PORTFOLIO_OR_ACCOUNT -> posting.getAccount() != null || posting.getPortfolio() != null;
        };

        if (!ownerPresent)
            issues.add(postingIssue(IssueCode.SEMANTIC_OWNER_REQUIRED,
                            LedgerDiagnosticCode.LEDGER_STRUCT_018
                                            .message("Semantic primary owner is missing for " + role), //$NON-NLS-1$
                            entry, posting).withDetail("projectionRole", role)); //$NON-NLS-1$
    }

    private static void validateUnitGrouping(LedgerEntry entry, List<ValidationIssue> issues)
    {
        var primaryGroupKeys = entry.getPostings().stream() //
                        .filter(posting -> posting.getUnitRole() == LedgerPostingUnitRole.PRIMARY) //
                        .map(LedgerPosting::getGroupKey) //
                        .filter(groupKey -> !isBlank(groupKey)) //
                        .collect(java.util.stream.Collectors.toSet());
        var primaryCount = entry.getPostings().stream()
                        .filter(posting -> posting.getUnitRole() == LedgerPostingUnitRole.PRIMARY).count();
        var repeatedUnitKeys = new HashSet<String>();
        var seenUnitKeys = new HashSet<String>();

        for (var posting : entry.getPostings())
        {
            if (!isUnit(posting))
                continue;

            if (primaryCount > 1 && primaryGroupKeys.size() > 1 && isBlank(posting.getGroupKey()))
                issues.add(postingIssue(IssueCode.SEMANTIC_UNIT_GROUP_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_019
                                                .message("Grouped unit posting requires a group key"), //$NON-NLS-1$
                                entry, posting));

            if (!primaryGroupKeys.isEmpty() && !isBlank(posting.getGroupKey())
                            && !primaryGroupKeys.contains(posting.getGroupKey()))
                issues.add(postingIssue(IssueCode.SEMANTIC_UNIT_GROUP_AMBIGUOUS,
                                LedgerDiagnosticCode.LEDGER_STRUCT_020
                                                .message("Unit posting group key has no semantic primary anchor"), //$NON-NLS-1$
                                entry, posting).withDetail("groupKey", posting.getGroupKey())); //$NON-NLS-1$

            var key = posting.getUnitRole() + ":" + posting.getGroupKey(); //$NON-NLS-1$
            if (!seenUnitKeys.add(key))
                repeatedUnitKeys.add(key);
        }

        for (var posting : entry.getPostings())
        {
            if (!isUnit(posting))
                continue;

            var key = posting.getUnitRole() + ":" + posting.getGroupKey(); //$NON-NLS-1$
            if (repeatedUnitKeys.contains(key) && isBlank(posting.getLocalKey()))
                issues.add(postingIssue(IssueCode.SEMANTIC_LOCAL_KEY_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_021
                                                .message("Repeated unit posting requires a local key"), //$NON-NLS-1$
                                entry, posting).withDetail("groupKey", posting.getGroupKey()) //$NON-NLS-1$
                                                .withDetail("unitRole", posting.getUnitRole())); //$NON-NLS-1$
        }
    }

    private static boolean isUnit(LedgerPosting posting)
    {
        var unitRole = posting.getUnitRole();
        return unitRole == LedgerPostingUnitRole.FEE || unitRole == LedgerPostingUnitRole.TAX
                        || unitRole == LedgerPostingUnitRole.GROSS_VALUE
                        || unitRole == LedgerPostingUnitRole.FOREX_CONTEXT;
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

    private enum OwnerKind
    {
        ACCOUNT,
        PORTFOLIO,
        PORTFOLIO_OR_ACCOUNT
    }
}
