package name.abuchen.portfolio.snapshot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.MutableMoney;

public class AssetCategory
{
    private final Classification classification;
    private final CurrencyConverter converter;
    private final LocalDateTime date;
    private final List<AssetPosition> positions = new ArrayList<>();
    private final Money totalAssets;
    private final MutableMoney valuation;

    /* package */AssetCategory(Classification classification, CurrencyConverter converter, LocalDateTime date,
                    Money totalAssets)
    {
        this.classification = classification;
        this.converter = converter;
        this.date = date;
        this.totalAssets = totalAssets;
        this.valuation = MutableMoney.of(converter.getTermCurrency());
    }

    public Money getValuation()
    {
        return this.valuation.toMoney();
    }

    public double getShare()
    {
        return (double) this.valuation.getAmount() / (double) this.totalAssets.getAmount();
    }

    public Money getFIFOPurchaseValue()
    {
        return positions.stream().map(AssetPosition::getFIFOPurchaseValue)
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    public Money getProfitLoss()
    {
        return positions.stream().map(AssetPosition::getProfitLoss)
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
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
    }
}
