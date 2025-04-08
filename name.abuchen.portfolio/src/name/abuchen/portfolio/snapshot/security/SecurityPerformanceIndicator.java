package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;

public interface SecurityPerformanceIndicator
{
    public interface Costs extends SecurityPerformanceIndicator
    {
        Money getFifoCost();

        Money getMovingAverageCost();

        Quote getFifoCostPerSharesHeld();

        Quote getMovingAverageCostPerSharesHeld();
    }

    public interface CapitalGains extends SecurityPerformanceIndicator
    {
        public Money getCapitalGains();

        public TrailRecord getCapitalGainsTrail();

        public Money getForexCaptialGains();

        public TrailRecord getForexCapitalGainsTrail();
    }

}
