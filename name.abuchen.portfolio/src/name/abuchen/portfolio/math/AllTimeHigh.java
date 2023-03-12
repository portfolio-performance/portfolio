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
    private LocalDate athDate;
    private Double athDistanceInPercent;

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

        if (!max.isPresent())
            return;

        SecurityPrice latest = security.getSecurityPrice(interval.getEnd());

        this.athValue = max.get().getValue();
        this.athDate = max.get().getDate();
        this.athDistanceInPercent = (latest.getValue() - max.get().getValue()) / (double) max.get().getValue();
    }

    public Long getValue()
    {
        return this.athValue;
    }

    public Double getDistance()
    {
        return this.athDistanceInPercent;
    }
    
    public LocalDate getDate()
    {
        return this.athDate;
    }
}
