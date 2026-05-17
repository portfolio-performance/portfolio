package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;

public interface SecurityPerformanceIndicator
{
    public interface Costs extends SecurityPerformanceIndicator
    {
        Money getCost(CostMethod method, TaxesAndFees taxesAndFees);

        Quote getCostPerSharesHeld(CostMethod costMethod);
    }

    public interface CapitalGains extends SecurityPerformanceIndicator
    {
        public Money getCapitalGains();

        public TrailRecord getCapitalGainsTrail();

        public Money getForexCaptialGains();

        public TrailRecord getForexCapitalGainsTrail();
    }

}
