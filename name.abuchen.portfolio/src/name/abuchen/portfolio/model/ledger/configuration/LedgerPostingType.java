package name.abuchen.portfolio.model.ledger.configuration;

import java.util.EnumSet;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;

/**
 * Defines stable Ledger posting codes used by persistence and validation.
 * This is Ledger configuration metadata. Existing persistence codes must stay stable, and
 * normal transaction-editing code should use higher-level write paths.
 *
 * <p>
 * Protobuf stores {@link #getCode()} in {@code PLedgerPosting.typeCode}. Existing codes
 * must never be changed or reused for a different posting meaning.
 * </p>
 */
@SuppressWarnings("nls")
public enum LedgerPostingType
{
    CASH("CASH", ComponentClass.CASH, Characteristic.MONEY_BEARING, Characteristic.CURRENCY_REQUIRED,
                    Characteristic.ACCOUNT_REFERENCE_MEANINGFUL, Characteristic.FOREX_MEANINGFUL),
    SECURITY("SECURITY", ComponentClass.SECURITY, Characteristic.MONEY_BEARING, Characteristic.CURRENCY_REQUIRED,
                    Characteristic.SECURITY_BEARING, Characteristic.SECURITY_REQUIRED, Characteristic.SHARES_MEANINGFUL,
                    Characteristic.PORTFOLIO_REFERENCE_MEANINGFUL, Characteristic.FOREX_MEANINGFUL),
    CASH_COMPENSATION("CASH_COMPENSATION", ComponentClass.COMPENSATION, Characteristic.MONEY_BEARING,
                    Characteristic.CURRENCY_REQUIRED, Characteristic.ACCOUNT_REFERENCE_MEANINGFUL,
                    Characteristic.FOREX_MEANINGFUL),
    FEE("FEE", ComponentClass.FEE, Characteristic.MONEY_BEARING, Characteristic.CURRENCY_REQUIRED,
                    Characteristic.FOREX_MEANINGFUL),
    TAX("TAX", ComponentClass.TAX, Characteristic.MONEY_BEARING, Characteristic.CURRENCY_REQUIRED,
                    Characteristic.FOREX_MEANINGFUL),
    GROSS_VALUE("GROSS_VALUE", ComponentClass.GROSS_VALUE, Characteristic.MONEY_BEARING, Characteristic.CURRENCY_REQUIRED,
                    Characteristic.FOREX_MEANINGFUL),
    FOREX("FOREX", ComponentClass.FOREX, Characteristic.FOREX_MEANINGFUL),
    RIGHT("RIGHT", ComponentClass.RIGHT, Characteristic.SECURITY_BEARING, Characteristic.SHARES_MEANINGFUL,
                    Characteristic.PORTFOLIO_REFERENCE_MEANINGFUL),
    BOND("BOND", ComponentClass.BOND, Characteristic.SECURITY_BEARING, Characteristic.SHARES_MEANINGFUL,
                    Characteristic.PORTFOLIO_REFERENCE_MEANINGFUL),
    ACCRUED_INTEREST("ACCRUED_INTEREST", ComponentClass.ACCRUED_INTEREST, Characteristic.MONEY_BEARING,
                    Characteristic.CURRENCY_REQUIRED, Characteristic.FOREX_MEANINGFUL),
    PRINCIPAL_REDEMPTION("PRINCIPAL_REDEMPTION", ComponentClass.PRINCIPAL_REDEMPTION, Characteristic.MONEY_BEARING,
                    Characteristic.CURRENCY_REQUIRED, Characteristic.FOREX_MEANINGFUL);

    public enum ComponentClass
    {
        CASH,
        SECURITY,
        COMPENSATION,
        FEE,
        TAX,
        GROSS_VALUE,
        FOREX,
        RIGHT,
        BOND,
        ACCRUED_INTEREST,
        PRINCIPAL_REDEMPTION
    }

    public enum Characteristic
    {
        MONEY_BEARING,
        CURRENCY_REQUIRED,
        SECURITY_BEARING,
        SECURITY_REQUIRED,
        SHARES_MEANINGFUL,
        ACCOUNT_REFERENCE_MEANINGFUL,
        PORTFOLIO_REFERENCE_MEANINGFUL,
        FOREX_MEANINGFUL
    }

    private final String code;
    private final ComponentClass componentClass;
    private final EnumSet<Characteristic> characteristics;

    private LedgerPostingType(String code, ComponentClass componentClass, Characteristic... characteristics)
    {
        this.code = Objects.requireNonNull(code);
        this.componentClass = Objects.requireNonNull(componentClass);
        this.characteristics = EnumSet.noneOf(Characteristic.class);

        for (var characteristic : characteristics)
            this.characteristics.add(Objects.requireNonNull(characteristic));
    }

    public String getCode()
    {
        return code;
    }

    public ComponentClass getComponentClass()
    {
        return componentClass;
    }

    public boolean isMoneyBearing()
    {
        return hasCharacteristic(Characteristic.MONEY_BEARING);
    }

    public boolean requiresCurrency()
    {
        return hasCharacteristic(Characteristic.CURRENCY_REQUIRED);
    }

    public boolean isSecurityBearing()
    {
        return hasCharacteristic(Characteristic.SECURITY_BEARING);
    }

    public boolean requiresSecurity()
    {
        return hasCharacteristic(Characteristic.SECURITY_REQUIRED);
    }

    public boolean isSharesMeaningful()
    {
        return hasCharacteristic(Characteristic.SHARES_MEANINGFUL);
    }

    public boolean isAccountReferenceMeaningful()
    {
        return hasCharacteristic(Characteristic.ACCOUNT_REFERENCE_MEANINGFUL);
    }

    public boolean isPortfolioReferenceMeaningful()
    {
        return hasCharacteristic(Characteristic.PORTFOLIO_REFERENCE_MEANINGFUL);
    }

    public boolean isForexMeaningful()
    {
        return hasCharacteristic(Characteristic.FOREX_MEANINGFUL);
    }

    public boolean hasCharacteristic(Characteristic characteristic)
    {
        return characteristics.contains(characteristic);
    }

    public static LedgerPostingType fromCode(String code)
    {
        for (LedgerPostingType type : values())
            if (type.code.equals(code))
                return type;

        throw new IllegalArgumentException(
                        LedgerDiagnosticCode.LEDGER_CORE_022.message("Unknown LedgerPostingType code: " + code)); //$NON-NLS-1$
    }
}
