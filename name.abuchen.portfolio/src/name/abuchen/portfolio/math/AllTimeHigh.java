package name.abuchen.portfolio.math;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.util.Interval;

public class AllTimeHigh
{
    private List<LocalDate> dates = new ArrayList<>();
    private List<Long> values = new ArrayList<>();
    private Long athValue;
    private LocalDate athDate;

    public AllTimeHigh(LocalDate[] dates, long[] values)
    {
        this.dates = Arrays.stream(dates).toList();
        this.values = Arrays.stream(values).boxed().toList();

        this.calculate();
    }

    public AllTimeHigh(PerformanceIndex index)
    {
        this(index.getDates(), index.getTotals());
    }

    public AllTimeHigh(Security security, Interval interval)
    {
        security.getPricesIncludingLatest().stream().filter(price -> interval.contains(price.getDate()))
                        .forEach(price -> {
            this.dates.add(price.getDate());
            this.values.add(price.getValue());
        });

        this.calculate();
    }

    private void calculate()
    {
        if (this.values.isEmpty() || this.dates.isEmpty() || this.dates.size() != this.values.size())
            return;

        this.athValue = Collections.max(this.values);

        int index = this.values.indexOf(this.athValue);
        this.athDate = this.dates.get(index);
    }

    public Long getValue()
    {
        return this.athValue;
    }

    public Long getLatestValue()
    {
        return this.values.get(this.values.size() - 1);
    }

    public Double getDistance()
    {
        if (this.athValue == null)
            return null;
        
        return (this.getLatestValue() - this.athValue) / (double) this.athValue;
    }
    
    public LocalDate getDate()
    {
        return this.athDate;
    }
}
