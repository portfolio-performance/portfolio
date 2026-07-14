package name.abuchen.portfolio.model.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.money.Money;

/**
 * A typed fact attached to a {@link LedgerEntry} or a {@link LedgerPosting}, such
 * as the ex-date of a dividend.
 * <p>
 * The {@link LedgerParameterType} names the fact and fixes the {@link ValueKind}
 * of its value; the factory methods accept a value only of the matching kind.
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
