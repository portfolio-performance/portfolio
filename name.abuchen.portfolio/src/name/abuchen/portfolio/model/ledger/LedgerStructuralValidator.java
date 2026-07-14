package name.abuchen.portfolio.model.ledger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerRoleRules;

/**
 * Validates the structural consistency of ledger entries against the semantic
 * shape defined by {@link LedgerRoleRules}. It does not repair entries and does
 * not guess missing data.
 * <p>
 * The stable identifier of a finding is its {@link LedgerIssueCode}; the message
 * is technical English and is not translated.
 */
@SuppressWarnings("nls")
public final class LedgerStructuralValidator
{
    private LedgerStructuralValidator()
    {
    }

    public static ValidationResult validate(Ledger ledger)
    {
        var issues = new ArrayList<ValidationIssue>();

        if (ledger == null)
        {
            issues.add(new ValidationIssue(LedgerIssueCode.LEDGER_REQUIRED, "Ledger is required"));
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
                issues.add(entryIssue(LedgerIssueCode.ENTRY_TYPE_REQUIRED,
                                "Entry type is required for entry " + entry.getUUID(), entry));

            if (entry.getDateTime() == null)
                issues.add(entryIssue(LedgerIssueCode.ENTRY_DATE_TIME_REQUIRED,
                                "Entry date/time is required for entry " + entry.getUUID(), entry));

            validateParameters(entry, null, entry.getParameters(), issues);

            validatePostings(entry, issues);
        }
    }

    private static void validatePostings(LedgerEntry entry, List<ValidationIssue> issues)
    {
        for (var posting : entry.getPostings())
        {
            if (posting.getType() == null)
                issues.add(postingIssue(LedgerIssueCode.POSTING_TYPE_REQUIRED,
                                "Posting type is required for posting " + posting.getUUID(), entry, posting));

            if (posting.getAmount() < 0 || posting.getShares() < 0)
                issues.add(postingIssue(LedgerIssueCode.SIGNED_FACT_NOT_ALLOWED,
                                "Negative " + negativeFacts(posting) + " on posting " + posting.getUUID()
                                                + ": amount and shares are unsigned, the movement is expressed by the direction",
                                entry, posting));

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
            issues.add(entryIssue(LedgerIssueCode.DIVIDEND_SECURITY_REQUIRED,
                            "Dividend entry requires a security on a posting: " + entry.getUUID(), entry));
    }

    private static void validatePostingShape(LedgerEntry entry, LedgerPosting posting, List<ValidationIssue> issues)
    {
        if (posting.getType() == null)
            return;

        if (posting.getType().requiresCurrency() && isBlank(posting.getCurrency()))
            issues.add(postingIssue(LedgerIssueCode.POSTING_CURRENCY_REQUIRED,
                            "Posting currency is required for posting " + posting.getUUID(), entry, posting));

        if (posting.getType().requiresSecurity() && posting.getSecurity() == null)
            issues.add(postingIssue(LedgerIssueCode.POSTING_SECURITY_REQUIRED,
                            "Posting security is required for posting " + posting.getUUID(), entry, posting));

        if (posting.getExchangeRate() != null && posting.getExchangeRate().signum() <= 0)
            issues.add(postingIssue(LedgerIssueCode.POSTING_EXCHANGE_RATE_POSITIVE,
                            "Posting exchange rate must be positive for posting " + posting.getUUID(), entry, posting));
    }

    private static void validateSemanticShape(LedgerEntry entry, List<ValidationIssue> issues)
    {
        if (entry.getType() == null)
            return;

        for (var rule : LedgerRoleRules.primaryRules(entry.getType()))
            requirePrimary(entry, rule, issues);
    }

    private static void requirePrimary(LedgerEntry entry, LedgerRoleRules.PrimaryRule rule,
                    List<ValidationIssue> issues)
    {
        var matches = entry.getPostings().stream() //
                        .filter(posting -> matchesPrimary(posting, rule)) //
                        .toList();

        if (matches.isEmpty())
        {
            issues.add(entryIssue(missingIssueCode(rule.projectionRole()),
                            "Required semantic primary posting is missing for " + rule.projectionRole(), entry)
                                            .withDetail("projectionRole", rule.projectionRole()));
            return;
        }

        if (matches.size() > 1)
            issues.add(entryIssue(ambiguousIssueCode(rule.projectionRole()),
                            "Semantic primary posting is ambiguous for " + rule.projectionRole(), entry)
                                            .withDetail("projectionRole", rule.projectionRole())
                                            .withDetail("actualCount", matches.size()));

        for (var posting : matches)
            validateOwner(entry, rule, posting, issues);
    }

    private static LedgerIssueCode missingIssueCode(LedgerProjectionRole role)
    {
        return switch (role)
        {
            case SOURCE_ACCOUNT, SOURCE_PORTFOLIO -> LedgerIssueCode.SEMANTIC_SOURCE_REQUIRED;
            case TARGET_ACCOUNT, TARGET_PORTFOLIO -> LedgerIssueCode.SEMANTIC_TARGET_REQUIRED;
            default -> LedgerIssueCode.SEMANTIC_PRIMARY_REQUIRED;
        };
    }

    private static LedgerIssueCode ambiguousIssueCode(LedgerProjectionRole role)
    {
        return switch (role)
        {
            case SOURCE_ACCOUNT, SOURCE_PORTFOLIO -> LedgerIssueCode.SEMANTIC_SOURCE_AMBIGUOUS;
            case TARGET_ACCOUNT, TARGET_PORTFOLIO -> LedgerIssueCode.SEMANTIC_TARGET_AMBIGUOUS;
            default -> LedgerIssueCode.SEMANTIC_PRIMARY_AMBIGUOUS;
        };
    }

    private static boolean matchesPrimary(LedgerPosting posting, LedgerRoleRules.PrimaryRule rule)
    {
        return posting.getType() == rule.postingType()
                        && posting.getUnitRole() == LedgerPostingUnitRole.PRIMARY
                        && posting.getSemanticRole() == rule.semanticRole()
                        && posting.getDirection() == rule.direction();
    }

    private static void validateOwner(LedgerEntry entry, LedgerRoleRules.PrimaryRule rule, LedgerPosting posting,
                    List<ValidationIssue> issues)
    {
        var ownerPresent = switch (rule.ownerKind())
        {
            case ACCOUNT -> posting.getAccount() != null;
            case PORTFOLIO -> posting.getPortfolio() != null;
        };

        if (!ownerPresent)
            issues.add(postingIssue(LedgerIssueCode.SEMANTIC_OWNER_REQUIRED,
                            "Semantic primary owner is missing for " + rule.projectionRole(), entry, posting)
                                            .withDetail("projectionRole", rule.projectionRole()));
    }

    private static void validateParameters(LedgerEntry entry, LedgerPosting posting,
                    List<LedgerParameter<?>> parameters, List<ValidationIssue> issues)
    {
        // parameters cannot carry a null type, value kind, or value: LedgerParameter
        // rejects those at construction time

        for (var parameter : parameters)
        {
            if (!parameter.getType().supportsValueKind(parameter.getValueKind()))
                issues.add(parameterValueKindIssue(entry, posting, parameter));

            if (posting != null && parameter.getType() == LedgerParameterType.EX_DATE
                            && posting.getSecurity() == null)
                issues.add(parameterIssue(LedgerIssueCode.EX_DATE_SECURITY_REQUIRED,
                                "Ex-date parameter requires a security on posting " + posting.getUUID(), entry, posting,
                                parameter));

            if (!isCompatible(parameter))
                issues.add(parameterIssue(LedgerIssueCode.PARAMETER_VALUE_KIND_MISMATCH,
                                "Parameter value does not match value kind " + parameter.getValueKind(), entry, posting,
                                parameter));
        }
    }

    private static String negativeFacts(LedgerPosting posting)
    {
        if (posting.getAmount() < 0 && posting.getShares() < 0)
            return "amount and shares";

        return posting.getAmount() < 0 ? "amount" : "shares";
    }

    private static boolean isCompatible(LedgerParameter<?> parameter)
    {
        return parameter.getValueKind().supportsValue(parameter.getValue());
    }

    private static boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }

    private static ValidationIssue entryIssue(LedgerIssueCode code, String message, LedgerEntry entry)
    {
        return new ValidationIssue(code, message).withEntry(entry);
    }

    private static ValidationIssue postingIssue(LedgerIssueCode code, String message, LedgerEntry entry,
                    LedgerPosting posting)
    {
        return entryIssue(code, message, entry).withPosting(posting);
    }

    private static ValidationIssue parameterIssue(LedgerIssueCode code, String message, LedgerEntry entry,
                    LedgerPosting posting, LedgerParameter<?> parameter)
    {
        return postingIssue(code, message, entry, posting).withParameter(parameter);
    }

    private static ValidationIssue parameterValueKindIssue(LedgerEntry entry, LedgerPosting posting,
                    LedgerParameter<?> parameter)
    {
        var type = parameter.getType();
        var code = type == LedgerParameterType.EX_DATE ? LedgerIssueCode.EX_DATE_VALUE_KIND_REQUIRED
                        : LedgerIssueCode.PARAMETER_VALUE_KIND_MISMATCH;

        return parameterIssue(code, "Parameter " + type + " must use value kind " + type.getExpectedValueKind(), entry,
                        posting, parameter)
                                        .withDetail("expectedValueKind", type.getExpectedValueKind())
                                        .withDetail("actualValueKind", parameter.getValueKind());
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
        var displayName = isBlank(name) ? "<missing>" : name;
        var displayUUID = isBlank(uuid) ? "<missing>" : uuid;

        return displayName + " (" + displayUUID + ")";
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

        return summary.length() > 120 ? summary.substring(0, 117) + "..." : summary;
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

        public boolean hasIssue(LedgerIssueCode code)
        {
            return issues.stream().anyMatch(issue -> issue.getCode() == code);
        }

        public String format()
        {
            if (issues.isEmpty())
                return "OK";

            var builder = new StringBuilder();

            for (var issue : issues)
            {
                if (!builder.isEmpty())
                    builder.append("\n\n");

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
        private final LedgerIssueCode code;
        private final String message;
        private final Map<String, String> details = new LinkedHashMap<>();

        private ValidationIssue(LedgerIssueCode code, String message)
        {
            this.code = code;
            this.message = message;
        }

        public LedgerIssueCode getCode()
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
                return "[" + code.getCode() + "] " + message;

            var builder = new StringBuilder();

            builder.append("[").append(code.getCode()).append("] ").append(message);
            appendGroup(builder, "Entry",
                            detail("UUID", "entryUUID"),
                            detail("Type", "entryType"),
                            detail("DateTime", "entryDateTime"));
            appendGroup(builder, "Posting",
                            detail("UUID", "postingUUID"),
                            detail("Type", "postingType"),
                            detail("Amount", "amount"),
                            detail("Currency", "currency"),
                            detail("Shares", "shares"),
                            detail("ExchangeRate", "exchangeRate"),
                            detail("Security", "security"),
                            detail("Account", "postingAccount"),
                            detail("Portfolio", "postingPortfolio"));
            appendGroup(builder, "Parameter",
                            detail("Type", "parameterType"),
                            detail("ExpectedValueKind", "expectedValueKind"),
                            detail("ValueKind", "actualValueKind"),
                            detail("ValueType", "actualValueType"),
                            detail("Value", "actualValue"));

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

            return withDetail("entryUUID", entry.getUUID())
                            .withDetail("entryType", entry.getType())
                            .withDetail("entryDateTime", entry.getDateTime());
        }

        private ValidationIssue withPosting(LedgerPosting posting)
        {
            if (posting == null)
                return this;

            return withDetail("postingUUID", posting.getUUID())
                            .withDetail("postingType", posting.getType())
                            .withDetail("amount", posting.getAmount())
                            .withDetail("currency", posting.getCurrency())
                            .withDetail("shares", posting.getShares())
                            .withDetail("exchangeRate", posting.getExchangeRate())
                            .withDetail("security", securitySummary(posting.getSecurity()))
                            .withDetail("postingAccount", ownerSummary(posting.getAccount()))
                            .withDetail("postingPortfolio", ownerSummary(posting.getPortfolio()));
        }

        private ValidationIssue withParameter(LedgerParameter<?> parameter)
        {
            if (parameter == null)
                return this;

            var value = parameter.getValue();

            return withDetail("parameterType", parameter.getType())
                            .withDetail("actualValueKind", parameter.getValueKind())
                            .withDetail("actualValueType", value == null ? null : value.getClass().getSimpleName())
                            .withDetail("actualValue", valueSummary(value));
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

            builder.append("\n\n  ").append(group).append(":");

            for (var groupDetail : groupDetails)
            {
                if (details.containsKey(groupDetail.key))
                    builder.append("\n    ").append(groupDetail.label).append(": ")
                                    .append(details.get(groupDetail.key));
            }
        }

        private String detailValue(Object value)
        {
            if (value == null)
                return "<missing>";

            var string = String.valueOf(value);

            return string.isBlank() ? "<missing>" : string;
        }
    }

    private record Detail(String label, String key)
    {
    }
}
