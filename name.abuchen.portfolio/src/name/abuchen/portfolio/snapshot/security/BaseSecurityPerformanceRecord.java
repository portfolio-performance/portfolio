package name.abuchen.portfolio.snapshot.security;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.util.Interval;

public class BaseSecurityPerformanceRecord
{
    protected final Client client;
    protected final Security security;

    protected final CurrencyConverter converter;
    protected final Interval interval;

    protected final List<CalculationLineItem> lineItems = new ArrayList<>();

    public BaseSecurityPerformanceRecord(Client client, Security security, CurrencyConverter converter,
                    Interval interval)
    {
        this.client = client;
        this.security = security;

        this.converter = converter;
        this.interval = interval;
    }

    public Security getSecurity()
    {
        return security;
    }

    /* package */void addLineItem(CalculationLineItem item)
    {
        this.lineItems.add(item);
    }

    public List<CalculationLineItem> getLineItems()
    {
        return lineItems;
    }

}
