package name.abuchen.portfolio.snapshot.security;

abstract class AbstractCapitalGainsCalculation extends Calculation
{
    protected CapitalGainsRecord realizedCapitalGains;
    protected CapitalGainsRecord unrealizedCapitalGains;

    @Override
    public void prepare()
    {
        this.realizedCapitalGains = new CapitalGainsRecord(getSecurity(), getTermCurrency());
        this.unrealizedCapitalGains = new CapitalGainsRecord(getSecurity(), getTermCurrency());
    }

    public CapitalGainsRecord getRealizedCapitalGains()
    {
        return realizedCapitalGains;
    }

    public CapitalGainsRecord getUnrealizedCapitalGains()
    {
        return unrealizedCapitalGains;
    }
}
