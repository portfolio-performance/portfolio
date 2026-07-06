package name.abuchen.portfolio.model.ledger.nativeentry;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Carries ratio input for ledger-native entry assembly.
 * This is internal native-entry infrastructure. It describes facts for assembly and does not
 * mutate Ledger truth by itself.
 *
 * <p>
 * This value object is not persisted directly. The assembler writes ratios as Ledger
 * parameters with stable parameter type codes.
 * </p>
 */
public final class Ratio
{
    private final BigDecimal numerator;
    private final BigDecimal denominator;

    private Ratio(BigDecimal numerator, BigDecimal denominator)
    {
        this.numerator = Objects.requireNonNull(numerator);
        this.denominator = Objects.requireNonNull(denominator);
    }

    public static Ratio of(BigDecimal numerator, BigDecimal denominator)
    {
        return new Ratio(numerator, denominator);
    }

    public BigDecimal getNumerator()
    {
        return numerator;
    }

    public BigDecimal getDenominator()
    {
        return denominator;
    }
}
