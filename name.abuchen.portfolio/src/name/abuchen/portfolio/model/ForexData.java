package name.abuchen.portfolio.model;

import java.math.BigDecimal;

public class ForexData
{
    private String baseCurrency;
    private String termCurrency;
    private BigDecimal exchangeRate;
    private long baseAmount;

    public String getBaseCurrency()
    {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency)
    {
        this.baseCurrency = baseCurrency;
    }

    public String getTermCurrency()
    {
        return termCurrency;
    }

    public void setTermCurrency(String termCurrency)
    {
        this.termCurrency = termCurrency;
    }

    public BigDecimal getExchangeRate()
    {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate)
    {
        this.exchangeRate = exchangeRate;
    }

    public long getBaseAmount()
    {
        return baseAmount;
    }

    public void setBaseAmount(long baseAmount)
    {
        this.baseAmount = baseAmount;
    }
}
