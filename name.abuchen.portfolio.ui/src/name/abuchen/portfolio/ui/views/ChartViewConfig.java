package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.snapshot.ReportingPeriod;

public class ChartViewConfig
{
    private final String uuid;

    private final ReportingPeriod period;

    public ChartViewConfig(String uuid, ReportingPeriod period)
    {
        this.uuid = uuid;
        this.period = period;
    }

    public String getUUID()
    {
        return uuid;
    }

    public ReportingPeriod getPeriod()
    {
        return period;
    }

}
