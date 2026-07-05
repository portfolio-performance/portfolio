package name.abuchen.portfolio.model.ledger;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;

/**
 * Encodes one entry-level Corporate Action basis allocation row as a stable Ledger
 * string parameter value.
 */
public final class CorporateActionBasisAllocation
{
    private static final String ROLE = "role"; //$NON-NLS-1$
    private static final String LOCAL_KEY = "localKey"; //$NON-NLS-1$
    private static final String GROUP_KEY = "groupKey"; //$NON-NLS-1$
    private static final String PERCENT = "percent"; //$NON-NLS-1$

    private final LedgerLegRole targetRole;
    private final String targetLocalKey;
    private final String targetGroupKey;
    private final BigDecimal percent;

    private CorporateActionBasisAllocation(LedgerLegRole targetRole, String targetLocalKey, String targetGroupKey,
                    BigDecimal percent)
    {
        this.targetRole = Objects.requireNonNull(targetRole);
        this.targetLocalKey = requireNonBlank(targetLocalKey, "Basis allocation target localKey is required"); //$NON-NLS-1$
        this.targetGroupKey = blankToNull(targetGroupKey);
        this.percent = percent;
    }

    public static CorporateActionBasisAllocation percentage(LedgerLegRole targetRole, String targetLocalKey,
                    String targetGroupKey, BigDecimal percent)
    {
        return new CorporateActionBasisAllocation(targetRole, targetLocalKey, targetGroupKey,
                        Objects.requireNonNull(percent));
    }

    public static CorporateActionBasisAllocation parse(String value)
    {
        var fields = parseFields(value);
        var role = LedgerLegRole.valueOf(required(fields, ROLE));
        var localKey = required(fields, LOCAL_KEY);
        var groupKey = fields.get(GROUP_KEY);
        var percentValue = fields.get(PERCENT);
        var percent = percentValue == null || percentValue.isBlank() ? null : new BigDecimal(percentValue);

        return new CorporateActionBasisAllocation(role, localKey, groupKey, percent);
    }

    public LedgerLegRole getTargetRole()
    {
        return targetRole;
    }

    public String getTargetLocalKey()
    {
        return targetLocalKey;
    }

    public Optional<String> getTargetGroupKey()
    {
        return Optional.ofNullable(targetGroupKey);
    }

    public Optional<BigDecimal> getPercent()
    {
        return Optional.ofNullable(percent);
    }

    public String toParameterValue()
    {
        var fields = new LinkedHashMap<String, String>();

        fields.put(ROLE, targetRole.name());
        fields.put(LOCAL_KEY, targetLocalKey);

        if (targetGroupKey != null)
            fields.put(GROUP_KEY, targetGroupKey);

        if (percent != null)
            fields.put(PERCENT, percent.toPlainString());

        return encode(fields);
    }

    private static String encode(LinkedHashMap<String, String> fields)
    {
        return fields.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue()) //$NON-NLS-1$
                        .reduce((left, right) -> left + ";" + right) //$NON-NLS-1$
                        .orElseThrow();
    }

    private static Map<String, String> parseFields(String value)
    {
        var fields = new LinkedHashMap<String, String>();

        if (value == null || value.isBlank())
            throw new IllegalArgumentException(
                            LedgerDiagnosticCode.LEDGER_CORE_026.message("Basis allocation value is required")); //$NON-NLS-1$

        for (var token : value.split(";")) //$NON-NLS-1$
        {
            var separator = token.indexOf('=');

            if (separator <= 0)
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_026
                                .message("Basis allocation field is malformed: " + token)); //$NON-NLS-1$

            fields.put(token.substring(0, separator), token.substring(separator + 1));
        }

        return fields;
    }

    private static String required(Map<String, String> fields, String key)
    {
        return requireNonBlank(fields.get(key), "Basis allocation field is required: " + key); //$NON-NLS-1$
    }

    private static String requireNonBlank(String value, String message)
    {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_026.message(message));

        return value;
    }

    private static String blankToNull(String value)
    {
        return value == null || value.isBlank() ? null : value;
    }
}
