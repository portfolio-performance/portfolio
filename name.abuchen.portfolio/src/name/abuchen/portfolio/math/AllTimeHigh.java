package name.abuchen.portfolio.math;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.util.Interval;

public class AllTimeHigh
{
    private Security security;
    private Interval interval;
    private Long athValue;
    private Long atlValue;
    private LocalDate athDate;
    private LocalDate atlDate;
    private Double athDistanceInPercent;
    private Double relDistFromLow;

    public AllTimeHigh(Security security, Interval interval)
    {
        this.security = security;
        this.interval = Objects.requireNonNull(interval);

        this.calculate();
    }

    private void calculate()
    {
        if (security == null || interval == null)
            return;

        Optional<SecurityPrice> max = security.getPricesIncludingLatest().stream() //
                        .filter(p -> interval.contains(p.getDate())) //
                        .max(Comparator.comparing(SecurityPrice::getValue));

        Optional<SecurityPrice> min = security.getPricesIncludingLatest().stream() //
                        .filter(p -> interval.contains(p.getDate())) //
                        .min(Comparator.comparing(SecurityPrice::getValue));

        if (!max.isPresent() || !min.isPresent())
            return;

        SecurityPrice latest = security.getSecurityPrice(interval.getEnd());

        this.athValue = max.get().getValue();
        this.athDate = max.get().getDate();
        this.atlValue = min.get().getValue();
        this.atlDate = min.get().getDate();
        this.athDistanceInPercent = (latest.getValue() - max.get().getValue()) / (double) max.get().getValue();
        this.relDistFromLow = (double) (latest.getValue() - atlValue) / (athValue - atlValue);
    }

    public Long getValue()
    {
        return this.athValue;
    }

    public Long getLow()
    {
        return this.atlValue;
    }

    public Long getHigh()
    {
        return this.athValue;
    }

    public LocalDate getLowDate()
    {
        return this.atlDate;
    }

    public LocalDate getHighDate()
    {
        return this.athDate;
    }

    public Double getDistance()
    {
        return this.athDistanceInPercent;
    }

    public Double getRelLowDistance()
    {
        return this.relDistFromLow;
    }

    public LocalDate getDate()
    {
        return this.athDate;
    }
}
