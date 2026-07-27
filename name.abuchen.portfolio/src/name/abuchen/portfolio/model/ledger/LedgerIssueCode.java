package name.abuchen.portfolio.model.ledger;

import java.util.Locale;

/**
 * Identifies a structural defect of a ledger entry.
 * <p>
 * The code is the stable identifier of the defect: it is part of the REST API
 * and must never be renamed or reused for a different condition. The message
 * that accompanies an issue is technical English and may change at any time - a
 * client branches on the code, not on the prose.
 */
public enum LedgerIssueCode
{
    LEDGER_REQUIRED,
    ENTRY_TYPE_REQUIRED,
    ENTRY_DATE_TIME_REQUIRED,
    POSTING_TYPE_REQUIRED,
    POSTING_CURRENCY_REQUIRED,
    POSTING_SECURITY_REQUIRED,
    POSTING_EXCHANGE_RATE_POSITIVE,
    DIVIDEND_SECURITY_REQUIRED,
    PARAMETER_VALUE_KIND_MISMATCH,
    EX_DATE_VALUE_KIND_REQUIRED,
    EX_DATE_SECURITY_REQUIRED,
    SIGNED_FACT_NOT_ALLOWED,
    SEMANTIC_PRIMARY_REQUIRED,
    SEMANTIC_PRIMARY_AMBIGUOUS,
    SEMANTIC_SOURCE_REQUIRED,
    SEMANTIC_TARGET_REQUIRED,
    SEMANTIC_SOURCE_AMBIGUOUS,
    SEMANTIC_TARGET_AMBIGUOUS,
    SEMANTIC_OWNER_REQUIRED;

    /**
     * The identifier as it appears on the wire, for example
     * {@code entry-type-required}.
     */
    public String getCode()
    {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
