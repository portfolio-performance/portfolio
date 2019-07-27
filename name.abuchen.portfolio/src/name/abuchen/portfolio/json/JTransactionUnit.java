package name.abuchen.portfolio.json;

import java.math.BigDecimal;

import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Values;

public class JTransactionUnit
{
    private Transaction.Unit.Type type;
    private double amount;
    private String fxCurrency;
    private Double fxAmount;
    private BigDecimal fxRateToBase;

    public Transaction.Unit.Type getType()
    {
        return type;
    }

    public void setType(Transaction.Unit.Type type)
    {
        this.type = type;
    }

    public double getAmount()
    {
        return amount;
    }

    public void setAmount(double amount)
    {
        this.amount = amount;
    }

    public String getFxCurrency()
    {
        return fxCurrency;
    }

    public void setFxCurrency(String fxCurrency)
    {
        this.fxCurrency = fxCurrency;
    }

    public Double getFxAmount()
    {
        return fxAmount;
    }

    public void setFxAmount(Double fxAmount)
    {
        this.fxAmount = fxAmount;
    }

    public BigDecimal getFxRateToBase()
    {
        return fxRateToBase;
    }

    public void setFxRateToBase(BigDecimal fxRateToBase)
    {
        this.fxRateToBase = fxRateToBase;
    }

    public static JTransactionUnit from(Unit unit)
    {
        JTransactionUnit u = new JTransactionUnit();
        u.type = unit.getType();
        u.amount = unit.getAmount().getAmount() / Values.Amount.divider();

        if (unit.getForex() != null)
        {
            u.fxCurrency = unit.getForex().getCurrencyCode();
            u.fxAmount = unit.getForex().getAmount() / Values.Amount.divider();
            u.fxRateToBase = unit.getExchangeRate();
        }

        return u;
    }
}
