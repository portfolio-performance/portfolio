package name.abuchen.portfolio.model.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.LedgerCode;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.money.Money;

/**
 * Represents an extra typed fact stored on a Ledger entry or posting.
 * This is internal Ledger model data. Contributor code should use the appropriate creator,
 * editor, converter, or native assembler instead of editing parameters directly.
 */
public class LedgerParameter<T>
{
    public enum ValueKind
    {
        STRING(String.class),
        DECIMAL(BigDecimal.class),
        LONG(Long.class),
        MONEY(Money.class),
        SECURITY(Security.class),
        ACCOUNT(Account.class),
        PORTFOLIO(Portfolio.class),
        BOOLEAN(Boolean.class),
        LOCAL_DATE(LocalDate.class),
        LOCAL_DATE_TIME(LocalDateTime.class);

        private final Class<?> valueType;

        private ValueKind(Class<?> valueType)
        {
            this.valueType = Objects.requireNonNull(valueType);
        }

        public Class<?> getValueType()
        {
            return valueType;
        }

        public boolean supportsValue(Object value)
        {
            return value != null && valueType.isInstance(value);
        }
    }

    private final LedgerParameterType type;
    private final ValueKind valueKind;
    private final T value;

    private LedgerParameter(LedgerParameterType type, ValueKind valueKind, T value)
    {
        this.type = Objects.requireNonNull(type);
        this.valueKind = Objects.requireNonNull(valueKind);
        this.value = Objects.requireNonNull(value);
    }

    public static LedgerParameter<String> ofString(LedgerParameterType type, String value)
    {
        return of(type, ValueKind.STRING, value);
    }

    public static LedgerParameter<String> ofCode(LedgerParameterType type, LedgerCode code)
    {
        Objects.requireNonNull(type).requireValueKind(ValueKind.STRING);
        Objects.requireNonNull(code);

        if (!type.hasCodeDomain())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_012
                            .message(type + " does not define a controlled code domain")); //$NON-NLS-1$

        if (type.getCodeDomain() != code.getDomain())
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_013.message(
                            type + " expects code domain " + type.getCodeDomain() + " but got " + code.getDomain())); //$NON-NLS-1$ //$NON-NLS-2$

        if (!type.supportsCode(code.getCode()))
            throw new IllegalArgumentException(
                            LedgerDiagnosticCode.LEDGER_CORE_014.message(type + " does not support code " + code.getCode())); //$NON-NLS-1$

        return ofString(type, code.getCode());
    }

    public static LedgerParameter<BigDecimal> ofDecimal(LedgerParameterType type, BigDecimal value)
    {
        return of(type, ValueKind.DECIMAL, value);
    }

    public static LedgerParameter<Long> ofLong(LedgerParameterType type, long value)
    {
        return of(type, ValueKind.LONG, Long.valueOf(value));
    }

    public static LedgerParameter<Money> ofMoney(LedgerParameterType type, Money value)
    {
        return of(type, ValueKind.MONEY, value);
    }

    public static LedgerParameter<Security> ofSecurity(LedgerParameterType type, Security value)
    {
        return of(type, ValueKind.SECURITY, value);
    }

    public static LedgerParameter<Account> ofAccount(LedgerParameterType type, Account value)
    {
        return of(type, ValueKind.ACCOUNT, value);
    }

    public static LedgerParameter<Portfolio> ofPortfolio(LedgerParameterType type, Portfolio value)
    {
        return of(type, ValueKind.PORTFOLIO, value);
    }

    public static LedgerParameter<Boolean> ofBoolean(LedgerParameterType type, Boolean value)
    {
        return of(type, ValueKind.BOOLEAN, value);
    }

    public static LedgerParameter<LocalDate> ofLocalDate(LedgerParameterType type, LocalDate value)
    {
        return of(type, ValueKind.LOCAL_DATE, value);
    }

    public static LedgerParameter<LocalDateTime> ofLocalDateTime(LedgerParameterType type,
                    LocalDateTime value)
    {
        return of(type, ValueKind.LOCAL_DATE_TIME, value);
    }

    private static <T> LedgerParameter<T> of(LedgerParameterType type, ValueKind valueKind, T value)
    {
        Objects.requireNonNull(type).requireValueKind(valueKind);
        return unchecked(type, valueKind, value);
    }

    static <T> LedgerParameter<T> unchecked(LedgerParameterType type, ValueKind valueKind, T value)
    {
        return new LedgerParameter<>(type, valueKind, value);
    }

    public LedgerParameterType getType()
    {
        return type;
    }

    public ValueKind getValueKind()
    {
        return valueKind;
    }

    public T getValue()
    {
        return value;
    }
}
