package name.abuchen.portfolio.snapshot;

import java.time.LocalDateTime;
import java.util.Comparator;

import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;

public class AssetPosition
{
    public static final class ByDescription implements Comparator<AssetPosition>
    {
        @Override
        public int compare(AssetPosition p1, AssetPosition p2)
        {
            return p1.getDescription().compareToIgnoreCase(p2.getDescription());
        }
    }

    private final SecurityPosition position;
    private final CurrencyConverter converter;
    private final LocalDateTime date;
    private final Money totalAssets;
    private final Money valuation;

    /* package */ AssetPosition(SecurityPosition position, CurrencyConverter converter, LocalDateTime date,
                    Money totalAssets)
    {
        this.position = position;
        this.converter = converter;
        this.date = date;
        this.totalAssets = totalAssets;
        this.valuation = position.calculateValue();
    }

    public Money getValuation()
    {
        return converter.convert(date, valuation);
    }

    public double getShare()
    {
        return (double) getValuation().getAmount() / (double) this.totalAssets.getAmount();
    }

    public Money getFIFOPurchaseValue()
    {
        return position.getFIFOPurchaseValue(converter.getTermCurrency());
    }

    public Money getProfitLoss()
    {
        // calculate profit/loss on the converted values to avoid rounding
        // differences that can happen when converting the profit/loss value
        // from the base currency

        if (position.getInvestmentVehicle() instanceof Security)
            return getValuation().subtract(getFIFOPurchaseValue());
        else
            return Money.of(converter.getTermCurrency(), 0);
    }

    public String getDescription()
    {
        return position.getInvestmentVehicle().getName();
    }

    public Security getSecurity()
    {
        return position.getSecurity();
    }

    public SecurityPosition getPosition()
    {
        return position;
    }

    public InvestmentVehicle getInvestmentVehicle()
    {
        return position.getInvestmentVehicle();
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "AssetPosition [" + position.getInvestmentVehicle() + ", date=" + date + ", valuation=" + valuation
                        + "]";
    }

}
