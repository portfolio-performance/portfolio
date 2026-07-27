package name.abuchen.portfolio.model.ledger.configuration;

import java.util.EnumSet;
import java.util.Objects;

/**
 * The kind of fact a posting records: cash, instrument, fee, tax, or gross
 * value.
 * <p>
 * The characteristics of a type say which fields a posting must carry
 * (currency, instrument) and which are meaningful for it (shares, cash or
 * investment account reference, forex). Codes are part of the REST API: they
 * must never be changed or reused for a different meaning.
 */
public enum LedgerPostingType
{
    CASH(Characteristic.MONEY_BEARING, Characteristic.CURRENCY_REQUIRED, Characteristic.ACCOUNT_REFERENCE_MEANINGFUL,
                    Characteristic.FOREX_MEANINGFUL), //
    SECURITY(Characteristic.MONEY_BEARING, Characteristic.CURRENCY_REQUIRED, Characteristic.SECURITY_BEARING,
                    Characteristic.SECURITY_REQUIRED, Characteristic.SHARES_MEANINGFUL,
                    Characteristic.PORTFOLIO_REFERENCE_MEANINGFUL, Characteristic.FOREX_MEANINGFUL), //
    FEE(Characteristic.MONEY_BEARING, Characteristic.CURRENCY_REQUIRED, Characteristic.FOREX_MEANINGFUL), //
    TAX(Characteristic.MONEY_BEARING, Characteristic.CURRENCY_REQUIRED, Characteristic.FOREX_MEANINGFUL), //
    GROSS_VALUE(Characteristic.MONEY_BEARING, Characteristic.CURRENCY_REQUIRED, Characteristic.FOREX_MEANINGFUL);

    public enum Characteristic
    {
        MONEY_BEARING, //
        CURRENCY_REQUIRED, //
        SECURITY_BEARING, //
        SECURITY_REQUIRED, //
        SHARES_MEANINGFUL, //
        ACCOUNT_REFERENCE_MEANINGFUL, //
        PORTFOLIO_REFERENCE_MEANINGFUL, //
        FOREX_MEANINGFUL
    }

    private final EnumSet<Characteristic> characteristics;

    private LedgerPostingType(Characteristic... characteristics)
    {
        this.characteristics = EnumSet.noneOf(Characteristic.class);

        for (var characteristic : characteristics)
            this.characteristics.add(Objects.requireNonNull(characteristic));
    }

    public String getCode()
    {
        return name();
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
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("Missing LedgerPostingType code"); //$NON-NLS-1$

        for (LedgerPostingType type : values())
            if (type.getCode().equals(code))
                return type;

        throw new IllegalArgumentException("Unknown LedgerPostingType code: " + code); //$NON-NLS-1$
    }
}
