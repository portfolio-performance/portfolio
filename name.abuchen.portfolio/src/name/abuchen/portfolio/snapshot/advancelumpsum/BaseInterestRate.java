package name.abuchen.portfolio.snapshot.advancelumpsum;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Basiszins according to § 18 Abs. 4 InvStG used to calculate the German
 * Vorabpauschale. The rate is in percent, e.g. 2.29 for 2.29 %.
 */
public record BaseInterestRate(int year, BigDecimal rate, Source source)
{
    public enum Source
    {
        OFFICIAL,
        MANUAL
    }

    /**
     * Rates published by the BMF in the Bundessteuerblatt.
     */
    @SuppressWarnings("nls")
    private static final Map<Integer, BigDecimal> OFFICIAL_RATES = Map.of( //
                    2018, new BigDecimal("0.87"), //
                    2019, new BigDecimal("0.52"), //
                    2020, new BigDecimal("0.07"), //
                    2021, new BigDecimal("-0.45"), //
                    2022, new BigDecimal("-0.05"), //
                    2023, new BigDecimal("2.55"), //
                    2024, new BigDecimal("2.29"), //
                    2025, new BigDecimal("2.53"), //
                    2026, new BigDecimal("3.20"));

    private static Optional<BaseInterestRate> official(int year)
    {
        var rate = OFFICIAL_RATES.get(year);
        return rate == null ? Optional.empty() : Optional.of(new BaseInterestRate(year, rate, Source.OFFICIAL));
    }

    /**
     * Resolves the rate for the given year. A manual rate takes precedence over
     * the official rate.
     */
    public static Optional<BaseInterestRate> resolve(int year, Optional<BigDecimal> manualRate)
    {
        return manualRate.map(rate -> new BaseInterestRate(year, rate, Source.MANUAL)).or(() -> official(year));
    }
}
