package name.abuchen.portfolio.model.ledger.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerRequirement;
import name.abuchen.portfolio.model.ledger.configuration.rule.LedgerRequirementGroup;

/**
 * Validates native Ledger entries against Java-owned entry and leg definitions.
 * This validator is separate from {@code LedgerStructuralValidator}: it checks
 * native business shape metadata for supported create and edit paths, while
 * structural validation remains the generic persisted-fact guard.
 */
public final class LedgerNativeEntryDefinitionValidator
{
    public enum IssueCode
    {
        ENTRY_REQUIRED,
        ENTRY_TYPE_REQUIRED,
        ENTRY_DEFINITION_MISSING,
        REQUIRED_ENTRY_PARAMETER_MISSING,
        ENTRY_PARAMETER_NOT_ALLOWED,
        REQUIRED_ALTERNATIVE_GROUP_MISSING,
        POSTING_TYPE_NOT_ALLOWED,
        LEG_CARDINALITY_VIOLATED,
        AMBIGUOUS_LEG_MATCH,
        REQUIRED_LEG_PARAMETER_MISSING,
        LEG_PARAMETER_NOT_ALLOWED,
        PARAMETER_PLACEMENT_INVALID,
        LEG_PARAMETER_VALUE_MISMATCH,
        REQUIRED_PROJECTION_MISSING,
        PROJECTION_PRIMARY_POSTING_REQUIRED,
        PROJECTION_PRIMARY_POSTING_MISMATCH,
        PROJECTION_POSTING_GROUP_REQUIRED,
        PROJECTION_POSTING_GROUP_NOT_FOUND
    }

    private LedgerNativeEntryDefinitionValidator()
    {
    }

    public static ValidationResult validate(LedgerEntry entry)
    {
        var issues = new ArrayList<ValidationIssue>();

        if (entry == null)
        {
            issues.add(new ValidationIssue(IssueCode.ENTRY_REQUIRED,
                            LedgerDiagnosticCode.LEDGER_STRUCT_035.message("Ledger entry is required"))); //$NON-NLS-1$
            return new ValidationResult(issues);
        }

        var entryType = entry.getType();
        if (entryType == null)
        {
            issues.add(issue(IssueCode.ENTRY_TYPE_REQUIRED,
                            LedgerDiagnosticCode.LEDGER_STRUCT_036.message("Ledger entry type is required"), entry)); //$NON-NLS-1$
            return new ValidationResult(issues);
        }

        if (!entryType.isLedgerNativeTargeted())
            return new ValidationResult(issues);

        var definition = LedgerEntryDefinitionRegistry.lookup(entryType);
        if (definition.isEmpty())
        {
            issues.add(issue(IssueCode.ENTRY_DEFINITION_MISSING,
                            LedgerDiagnosticCode.LEDGER_STRUCT_037
                                            .message("Missing native Ledger entry definition for " + entryType), //$NON-NLS-1$
                            entry));
            return new ValidationResult(issues);
        }

        validateEntryParameters(entry, definition.get(), issues);
        validatePostingTypes(entry, definition.get(), issues);
        validateAlternativeGroups(entry, definition.get(), issues);
        validateLegs(entry, definition.get(), issues);

        return new ValidationResult(issues);
    }

    public static void assertValid(LedgerEntry entry)
    {
        var result = validate(entry);

        if (!result.isOK())
            throw new ValidationException(result);
    }

    private static void validateEntryParameters(LedgerEntry entry, LedgerEntryDefinition definition,
                    List<ValidationIssue> issues)
    {
        var entryParameterTypes = parameterTypes(entry.getParameters());

        for (var rule : definition.getRequiredEntryParameterRules())
        {
            if (!entryParameterTypes.contains(rule.getParameterType()))
                issues.add(issue(IssueCode.REQUIRED_ENTRY_PARAMETER_MISSING,
                                LedgerDiagnosticCode.LEDGER_STRUCT_038.message(
                                                "Required native entry parameter is missing: " //$NON-NLS-1$
                                                                + rule.getParameterType()),
                                entry)
                                                .withDetail("parameterType", rule.getParameterType())); //$NON-NLS-1$
        }

        for (var parameter : entry.getParameters())
        {
            var parameterType = parameter.getType();

            if (parameterType != null && !definition.getEntryParameterTypes().contains(parameterType))
                issues.add(issue(IssueCode.ENTRY_PARAMETER_NOT_ALLOWED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_039.message(
                                                "Entry parameter is not allowed for " + definition.getEntryType() + ": " //$NON-NLS-1$ //$NON-NLS-2$
                                                                + parameterType),
                                entry).withParameter(parameter));
        }
    }

    private static void validatePostingTypes(LedgerEntry entry, LedgerEntryDefinition definition,
                    List<ValidationIssue> issues)
    {
        for (var posting : entry.getPostings())
        {
            var postingType = posting.getType();

            if (postingType != null && !definition.getPostingTypes().contains(postingType))
                issues.add(issue(IssueCode.POSTING_TYPE_NOT_ALLOWED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_040.message(
                                                "Posting type is not allowed for " + definition.getEntryType() + ": " //$NON-NLS-1$ //$NON-NLS-2$
                                                                + postingType),
                                entry).withPosting(posting));
        }
    }

    private static void validateAlternativeGroups(LedgerEntry entry, LedgerEntryDefinition definition,
                    List<ValidationIssue> issues)
    {
        for (var group : definition.getAlternativeRequirementGroups())
        {
            if (group.getRequirement() == LedgerRequirement.REQUIRED && !isSatisfied(entry, group))
                issues.add(issue(IssueCode.REQUIRED_ALTERNATIVE_GROUP_MISSING,
                                LedgerDiagnosticCode.LEDGER_STRUCT_041.message(
                                                "Required native alternative group is missing: " + group.getName()), //$NON-NLS-1$
                                entry)
                                                .withDetail("groupName", group.getName())); //$NON-NLS-1$
        }
    }

    private static boolean isSatisfied(LedgerEntry entry, LedgerRequirementGroup group)
    {
        if (!group.getParameterTypes().isEmpty())
        {
            for (var type : group.getParameterTypes())
            {
                if (hasParameter(entry.getParameters(), type))
                    return true;

                for (var posting : entry.getPostings())
                    if (hasParameter(posting.getParameters(), type))
                        return true;
            }
        }

        if (!group.getPostingTypes().isEmpty())
            return entry.getPostings().stream().map(LedgerPosting::getType)
                            .anyMatch(group.getPostingTypes()::contains);

        return false;
    }

    private static void validateLegs(LedgerEntry entry, LedgerEntryDefinition definition,
                    List<ValidationIssue> issues)
    {
        var postingsByUUID = entry.getPostings().stream() //
                        .collect(Collectors.toMap(LedgerPosting::getUUID, posting -> posting, (left, right) -> left,
                                        LinkedHashMap::new));

        for (var leg : definition.getLegDefinitions())
        {
            var match = matchLeg(entry, definition, leg, postingsByUUID, issues);

            validateCardinality(entry, leg, match.postings(), issues);
            validateLegParameters(entry, leg, match.postings(), issues);
        }
    }

    private static LegMatch matchLeg(LedgerEntry entry, LedgerEntryDefinition definition, LedgerLegDefinition leg,
                    Map<String, LedgerPosting> postingsByUUID, List<ValidationIssue> issues)
    {
        var projectionRole = leg.getProjectionRole();

        if (projectionRole.isPresent())
            return matchProjectedLeg(entry, definition, leg, projectionRole.get(), postingsByUUID, issues);

        return new LegMatch(entry.getPostings().stream() //
                        .filter(posting -> postingMatchesLeg(entry.getType(), posting, leg)).toList());
    }

    private static LegMatch matchProjectedLeg(LedgerEntry entry, LedgerEntryDefinition definition,
                    LedgerLegDefinition leg, LedgerProjectionRole projectionRole, Map<String, LedgerPosting> postingsByUUID,
                    List<ValidationIssue> issues)
    {
        var refs = entry.getProjectionRefs().stream().filter(ref -> ref.getRole() == projectionRole).toList();
        var sameTypePostings = entry.getPostings().stream().filter(posting -> posting.getType() == leg.getPostingType())
                        .toList();
        var matchingPostings = new ArrayList<LedgerPosting>();

        if (refs.isEmpty() && (requiresLeg(leg.getCardinality()) || !sameTypePostings.isEmpty()))
            issues.add(issue(IssueCode.REQUIRED_PROJECTION_MISSING,
                            LedgerDiagnosticCode.LEDGER_STRUCT_042
                                            .message("Native leg projection is missing: " + projectionRole), //$NON-NLS-1$
                            entry)
                                            .withDetail("legRole", leg.getRole()) //$NON-NLS-1$
                                            .withDetail("projectionRole", projectionRole)); //$NON-NLS-1$

        for (var ref : refs)
        {
            if (leg.isPrimaryPostingExpected() && isBlank(ref.getPrimaryPostingUUID()))
            {
                issues.add(issue(IssueCode.PROJECTION_PRIMARY_POSTING_REQUIRED,
                                LedgerDiagnosticCode.LEDGER_STRUCT_043.message(
                                                "Native leg projection requires a primary posting: " + projectionRole), //$NON-NLS-1$
                                entry)
                                                .withProjection(ref)
                                                .withDetail("legRole", leg.getRole())); //$NON-NLS-1$
                continue;
            }

            var posting = postingsByUUID.get(ref.getPrimaryPostingUUID());
            if (posting != null)
            {
                if (postingMatchesLeg(entry.getType(), posting, leg))
                    matchingPostings.add(posting);
                else
                    issues.add(issue(IssueCode.PROJECTION_PRIMARY_POSTING_MISMATCH,
                                    LedgerDiagnosticCode.LEDGER_STRUCT_044.message(
                                                    "Projection primary posting does not match native leg " //$NON-NLS-1$
                                                                    + leg.getRole()),
                                    entry)
                                                    .withProjection(ref).withPosting(posting)
                                                    .withDetail("legRole", leg.getRole())); //$NON-NLS-1$
            }

            if (leg.isPostingGroupExpected())
            {
                if (isBlank(ref.getPostingGroupUUID()))
                    issues.add(issue(IssueCode.PROJECTION_POSTING_GROUP_REQUIRED,
                                    LedgerDiagnosticCode.LEDGER_STRUCT_045.message(
                                                    "Native leg projection requires a posting group anchor: " //$NON-NLS-1$
                                                                    + projectionRole),
                                    entry).withProjection(ref).withDetail("legRole", leg.getRole())); //$NON-NLS-1$
                else if (!postingsByUUID.containsKey(ref.getPostingGroupUUID()))
                    issues.add(issue(IssueCode.PROJECTION_POSTING_GROUP_NOT_FOUND,
                                    LedgerDiagnosticCode.LEDGER_STRUCT_046.message(
                                                    "Native leg projection posting group anchor does not exist: " //$NON-NLS-1$
                                                                    + ref.getPostingGroupUUID()),
                                    entry).withProjection(ref).withDetail("legRole", leg.getRole())); //$NON-NLS-1$
            }
        }

        if (refs.size() > 1 && leg.getCardinality() != LedgerLegCardinality.REPEATABLE
                        && leg.getCardinality() != LedgerLegCardinality.AT_LEAST_ONE)
            issues.add(issue(IssueCode.AMBIGUOUS_LEG_MATCH,
                            LedgerDiagnosticCode.LEDGER_STRUCT_047
                                            .message("Native leg maps to multiple projection refs: " + leg.getRole()), //$NON-NLS-1$
                            entry)
                                            .withDetail("legRole", leg.getRole()) //$NON-NLS-1$
                                            .withDetail("projectionRole", projectionRole)); //$NON-NLS-1$

        validateAllowedProjectionRole(definition, entry, leg, projectionRole, issues);

        return new LegMatch(matchingPostings);
    }

    private static void validateAllowedProjectionRole(LedgerEntryDefinition definition, LedgerEntry entry,
                    LedgerLegDefinition leg, LedgerProjectionRole projectionRole, List<ValidationIssue> issues)
    {
        if (!definition.getProjectionRoles().contains(projectionRole))
            issues.add(issue(IssueCode.REQUIRED_PROJECTION_MISSING,
                            LedgerDiagnosticCode.LEDGER_STRUCT_048.message(
                                            "Native leg projection role is not allowed by entry definition: " //$NON-NLS-1$
                                                            + projectionRole),
                            entry)
                                            .withDetail("legRole", leg.getRole()) //$NON-NLS-1$
                                            .withDetail("projectionRole", projectionRole)); //$NON-NLS-1$
    }

    private static boolean requiresLeg(LedgerLegCardinality cardinality)
    {
        return cardinality == LedgerLegCardinality.EXACTLY_ONE || cardinality == LedgerLegCardinality.AT_LEAST_ONE;
    }

    private static void validateCardinality(LedgerEntry entry, LedgerLegDefinition leg, List<LedgerPosting> postings,
                    List<ValidationIssue> issues)
    {
        var count = postings.size();

        switch (leg.getCardinality())
        {
            case EXACTLY_ONE:
                if (count != 1)
                    issues.add(cardinalityIssue(LedgerDiagnosticCode.LEDGER_STRUCT_049, entry, leg, count,
                                    "exactly one")); //$NON-NLS-1$
                break;
            case AT_LEAST_ONE:
                if (count < 1)
                    issues.add(cardinalityIssue(LedgerDiagnosticCode.LEDGER_STRUCT_050, entry, leg, count,
                                    "at least one")); //$NON-NLS-1$
                break;
            case OPTIONAL:
                if (count > 1)
                    issues.add(issue(IssueCode.AMBIGUOUS_LEG_MATCH,
                                    LedgerDiagnosticCode.LEDGER_STRUCT_051.message(
                                                    "Optional native leg maps to multiple postings: " + leg.getRole()), //$NON-NLS-1$
                                    entry)
                                                    .withDetail("legRole", leg.getRole()) //$NON-NLS-1$
                                                    .withDetail("actualCount", count)); //$NON-NLS-1$
                break;
            case REPEATABLE:
                break;
            default:
                throw new IllegalStateException(LedgerDiagnosticCode.LEDGER_STRUCT_052
                                .message("Unhandled LedgerLegCardinality: " + leg.getCardinality())); //$NON-NLS-1$
        }
    }

    private static ValidationIssue cardinalityIssue(LedgerDiagnosticCode diagnosticCode, LedgerEntry entry,
                    LedgerLegDefinition leg, int actual, String expected)
    {
        return issue(IssueCode.LEG_CARDINALITY_VIOLATED,
                        diagnosticCode.message(
                                        "Native leg cardinality violated for " + leg.getRole() + ": expected " //$NON-NLS-1$ //$NON-NLS-2$
                                                        + expected + ", actual " + actual), //$NON-NLS-1$
                        entry).withDetail("legRole", leg.getRole()) //$NON-NLS-1$
                                        .withDetail("expectedCardinality", leg.getCardinality()) //$NON-NLS-1$
                                        .withDetail("actualCount", actual); //$NON-NLS-1$
    }

    private static void validateLegParameters(LedgerEntry entry, LedgerLegDefinition leg, List<LedgerPosting> postings,
                    List<ValidationIssue> issues)
    {
        var allowed = EnumSet.noneOf(LedgerParameterType.class);
        allowed.addAll(leg.getRequiredParameterTypes());
        allowed.addAll(leg.getOptionalParameterTypes());

        for (var posting : postings)
        {
            for (var required : leg.getRequiredParameterTypes())
            {
                if (!hasParameter(posting.getParameters(), required))
                {
                    var misplaced = hasParameter(entry.getParameters(), required)
                                    || entry.getPostings().stream().filter(other -> other != posting)
                                                    .anyMatch(other -> hasParameter(other.getParameters(), required));

                    var code = misplaced ? IssueCode.PARAMETER_PLACEMENT_INVALID
                                    : IssueCode.REQUIRED_LEG_PARAMETER_MISSING;
                    issues.add(issue(code,
                                    LedgerDiagnosticCode.LEDGER_STRUCT_053.message(
                                                    "Required native leg parameter is missing from posting: " //$NON-NLS-1$
                                                                    + required),
                                    entry)
                                                    .withPosting(posting)
                                                    .withDetail("legRole", leg.getRole()) //$NON-NLS-1$
                                                    .withDetail("parameterType", required)); //$NON-NLS-1$
                }
            }

            for (var parameter : posting.getParameters())
            {
                var parameterType = parameter.getType();

                if (parameterType != null && !allowed.contains(parameterType))
                    issues.add(issue(IssueCode.LEG_PARAMETER_NOT_ALLOWED,
                                    LedgerDiagnosticCode.LEDGER_STRUCT_054.message(
                                                    "Native leg parameter is not allowed for " + leg.getRole() + ": " //$NON-NLS-1$ //$NON-NLS-2$
                                                                    + parameterType),
                                    entry).withPosting(posting).withParameter(parameter)
                                                    .withDetail("legRole", leg.getRole())); //$NON-NLS-1$
            }

            validateExpectedLegCode(entry, leg, posting, issues);
        }
    }

    private static void validateExpectedLegCode(LedgerEntry entry, LedgerLegDefinition leg, LedgerPosting posting,
                    List<ValidationIssue> issues)
    {
        var expected = expectedCorporateActionLegCode(entry.getType(), leg.getRole());

        if (expected.isEmpty())
            return;

        var value = parameterValue(posting.getParameters(), LedgerParameterType.CORPORATE_ACTION_LEG);

        if (value.isPresent() && !expected.get().equals(value.get()))
            issues.add(issue(IssueCode.LEG_PARAMETER_VALUE_MISMATCH,
                            LedgerDiagnosticCode.LEDGER_STRUCT_055
                                            .message("Native leg has unexpected CORPORATE_ACTION_LEG value"), //$NON-NLS-1$
                            entry)
                                            .withPosting(posting)
                                            .withDetail("legRole", leg.getRole()) //$NON-NLS-1$
                                            .withDetail("expectedValue", expected.get()) //$NON-NLS-1$
                                            .withDetail("actualValue", value.get())); //$NON-NLS-1$
    }

    private static boolean postingMatchesLeg(LedgerEntryType entryType, LedgerPosting posting,
                    LedgerLegDefinition leg)
    {
        if (posting.getType() != leg.getPostingType())
            return false;

        var expectedLegCode = expectedCorporateActionLegCode(entryType, leg.getRole());

        return expectedLegCode.isEmpty() || parameterValue(posting.getParameters(), LedgerParameterType.CORPORATE_ACTION_LEG)
                        .filter(expectedLegCode.get()::equals).isPresent();
    }

    private static Optional<String> expectedCorporateActionLegCode(LedgerEntryType entryType, LedgerLegRole role)
    {
        if (entryType == LedgerEntryType.SPIN_OFF)
        {
            if (role == LedgerLegRole.SOURCE_SECURITY_LEG)
                return Optional.of(CorporateActionLeg.SOURCE_SECURITY.getCode());
            if (role == LedgerLegRole.TARGET_SECURITY_LEG)
                return Optional.of(CorporateActionLeg.TARGET_SECURITY.getCode());
        }

        if (entryType == LedgerEntryType.STOCK_DIVIDEND || entryType == LedgerEntryType.BONUS_ISSUE)
        {
            if (role == LedgerLegRole.RECEIVED_SECURITY_LEG)
                return Optional.of(CorporateActionLeg.TARGET_SECURITY.getCode());
        }

        if (entryType == LedgerEntryType.RIGHTS_DISTRIBUTION)
        {
            if (role == LedgerLegRole.DISTRIBUTED_RIGHT_LEG)
                return Optional.of(CorporateActionLeg.RIGHT_SECURITY.getCode());
            if (role == LedgerLegRole.DISTRIBUTED_SECURITY_LEG)
                return Optional.of(CorporateActionLeg.DISTRIBUTED_SECURITY.getCode());
            if (role == LedgerLegRole.SOURCE_SECURITY_LEG)
                return Optional.of(CorporateActionLeg.SOURCE_SECURITY.getCode());
        }

        if (entryType == LedgerEntryType.BOND_CONVERSION)
        {
            if (role == LedgerLegRole.SOURCE_BOND_LEG)
                return Optional.of(CorporateActionLeg.CONVERSION_SOURCE.getCode());
            if (role == LedgerLegRole.TARGET_SECURITY_LEG)
                return Optional.of(CorporateActionLeg.CONVERSION_TARGET.getCode());
        }

        if (role == LedgerLegRole.CASH_COMPENSATION_LEG)
            return Optional.of(CorporateActionLeg.CASH_COMPENSATION.getCode());
        if (role == LedgerLegRole.FEE_LEG)
            return Optional.of(CorporateActionLeg.FEE.getCode());
        if (role == LedgerLegRole.TAX_LEG)
            return Optional.of(CorporateActionLeg.TAX.getCode());
        if (role == LedgerLegRole.ACCRUED_INTEREST_LEG)
            return Optional.of(CorporateActionLeg.ACCRUED_INTEREST.getCode());

        return Optional.empty();
    }

    private static Set<LedgerParameterType> parameterTypes(List<LedgerParameter<?>> parameters)
    {
        var values = EnumSet.noneOf(LedgerParameterType.class);

        parameters.stream().map(LedgerParameter::getType).filter(Objects::nonNull).forEach(values::add);

        return values;
    }

    private static boolean hasParameter(List<LedgerParameter<?>> parameters, LedgerParameterType type)
    {
        return parameters.stream().anyMatch(parameter -> parameter.getType() == type);
    }

    private static Optional<String> parameterValue(List<LedgerParameter<?>> parameters, LedgerParameterType type)
    {
        return parameters.stream() //
                        .filter(parameter -> parameter.getType() == type) //
                        .map(LedgerParameter::getValue) //
                        .filter(String.class::isInstance) //
                        .map(String.class::cast) //
                        .findFirst();
    }

    private static boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }

    private static ValidationIssue issue(IssueCode code, String message, LedgerEntry entry)
    {
        return new ValidationIssue(code, message).withEntry(entry);
    }

    private record LegMatch(List<LedgerPosting> postings)
    {
        private LegMatch
        {
            postings = List.copyOf(postings);
        }
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

            return issues.stream().map(ValidationIssue::format).collect(Collectors.joining("\n\n")); //$NON-NLS-1$
        }
    }

    public static final class ValidationIssue
    {
        private final IssueCode code;
        private final String message;
        private final Map<String, String> details = new LinkedHashMap<>();

        private ValidationIssue(IssueCode code, String message)
        {
            this.code = Objects.requireNonNull(code);
            this.message = Objects.requireNonNull(message);
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

            for (var entry : details.entrySet())
                builder.append("\n  ").append(entry.getKey()).append(": ").append(entry.getValue()); //$NON-NLS-1$ //$NON-NLS-2$

            return builder.toString();
        }

        private ValidationIssue withEntry(LedgerEntry entry)
        {
            if (entry == null)
                return this;

            return withDetail("entryUUID", entry.getUUID()) //$NON-NLS-1$
                            .withDetail("entryType", entry.getType()); //$NON-NLS-1$
        }

        private ValidationIssue withPosting(LedgerPosting posting)
        {
            if (posting == null)
                return this;

            return withDetail("postingUUID", posting.getUUID()) //$NON-NLS-1$
                            .withDetail("postingType", posting.getType()); //$NON-NLS-1$
        }

        private ValidationIssue withProjection(LedgerProjectionRef projectionRef)
        {
            if (projectionRef == null)
                return this;

            return withDetail("projectionUUID", projectionRef.getUUID()) //$NON-NLS-1$
                            .withDetail("projectionRole", projectionRef.getRole()) //$NON-NLS-1$
                            .withDetail("primaryPostingUUID", projectionRef.getPrimaryPostingUUID()) //$NON-NLS-1$
                            .withDetail("postingGroupUUID", projectionRef.getPostingGroupUUID()); //$NON-NLS-1$
        }

        private ValidationIssue withParameter(LedgerParameter<?> parameter)
        {
            if (parameter == null)
                return this;

            return withDetail("parameterType", parameter.getType()) //$NON-NLS-1$
                            .withDetail("parameterValue", parameter.getValue()); //$NON-NLS-1$
        }

        private ValidationIssue withDetail(String key, Object value)
        {
            details.put(key, detailValue(value));
            return this;
        }

        private String detailValue(Object value)
        {
            if (value == null)
                return "<missing>"; //$NON-NLS-1$

            var string = String.valueOf(value);

            return string.isBlank() ? "<missing>" : string; //$NON-NLS-1$
        }
    }

    public static final class ValidationException extends IllegalArgumentException
    {
        private static final long serialVersionUID = 1L;

        private final ValidationResult result;

        private ValidationException(ValidationResult result)
        {
            super("Invalid native Ledger entry definition: " + result.format()); //$NON-NLS-1$
            this.result = result;
        }

        public ValidationResult getResult()
        {
            return result;
        }
    }
}
