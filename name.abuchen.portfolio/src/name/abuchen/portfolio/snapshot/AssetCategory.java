package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;

public class AssetCategory
{
    private final Classification classification;
    private final CurrencyConverter converter;
    private final LocalDate date;
    private final List<AssetPosition> positions = new ArrayList<>();
    private final Money totalAssets;
    private final MutableMoney valuation;
    private final MutableMoney notionalValuation;

    /* package */ AssetCategory(Classification classification, CurrencyConverter converter, LocalDate date,
                    Money totalAssets)
    {
        this.classification = classification;
        this.converter = converter;
        this.date = date;
        this.totalAssets = totalAssets;
        this.valuation = MutableMoney.of(converter.getTermCurrency());
        this.notionalValuation = MutableMoney.of(converter.getTermCurrency());
    }

    /**
     * Returns the market value of all positions in this category.
     */
    public Money getValuation()
    {
        return this.valuation.toMoney();
    }

    /**
     * Returns the notional exposure of all positions in this category, i.e.
     * the market value multiplied by the multiplier of each security.
     */
    public Money getNotionalValuation()
    {
        return this.notionalValuation.toMoney();
    }

    /**
     * Returns the share of the market value of this category in the total
     * assets (also based on market value).
     */
    public double getShare()
    {
        return (double) this.valuation.getAmount() / (double) this.totalAssets.getAmount();
    }

    public Classification getClassification()
    {
        return this.classification;
    }

    public List<AssetPosition> getPositions()
    {
        return positions;
    }

    public void addPosition(AssetPosition p)
    {
        this.positions.add(p);
        this.valuation.add(converter.convert(date, p.getValuation()));
        this.notionalValuation.add(converter.convert(date, p.getNotionalValuation()));
    }
}
