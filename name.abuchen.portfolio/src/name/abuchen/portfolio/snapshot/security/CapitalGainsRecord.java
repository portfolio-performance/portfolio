package name.abuchen.portfolio.snapshot.security;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;

public final class CapitalGainsRecord implements SecurityPerformanceIndicator.CapitalGains
{
    private final Security security;

    private Money capitalGains;
    private TrailRecord capitalGainsTrail;
    private Money forexCaptialGains;
    private TrailRecord forexCapitalGainsTrail;

    /* package */ CapitalGainsRecord(Security security, String termCurrency)
    {
        this.security = security;

        Money zero = Money.of(termCurrency, 0);

        this.capitalGains = zero;
        this.capitalGainsTrail = TrailRecord.empty();
        this.forexCaptialGains = zero;
        this.forexCapitalGainsTrail = TrailRecord.empty();
    }

    public Security getSecurity()
    {
        return security;
    }

    @Override
    public Money getCapitalGains()
    {
        return capitalGains;
    }

    @Override
    public TrailRecord getCapitalGainsTrail()
    {
        return capitalGainsTrail;
    }

    @Override
    public Money getForexCaptialGains()
    {
        return forexCaptialGains;
    }

    @Override
    public TrailRecord getForexCapitalGainsTrail()
    {
        return forexCapitalGainsTrail;
    }

    public void addCapitalGains(Money other)
    {
        this.capitalGains = this.capitalGains.add(other);
    }

    void addCapitalGainsTrail(TrailRecord other)
    {
        this.capitalGainsTrail = this.capitalGainsTrail.add(other);
    }

    void addForexCaptialGains(Money other)
    {
        this.forexCaptialGains = this.forexCaptialGains.add(other);
    }

    void addForexCapitalGainsTrail(TrailRecord other)
    {
        this.forexCapitalGainsTrail = this.forexCapitalGainsTrail.add(other);
    }
}
