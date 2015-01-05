package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;

public class PortfolioSnapshot
{
    // //////////////////////////////////////////////////////////////
    // factory methods
    // //////////////////////////////////////////////////////////////

    @Deprecated
    public static PortfolioSnapshot create(Portfolio portfolio, Date time)
    {
        CurrencyConverter converter = new CurrencyConverterImpl(null, CurrencyUnit.EUR, time);
        return create(portfolio, converter, time);
    }

    public static PortfolioSnapshot create(Portfolio portfolio, CurrencyConverter converter, Date time)
    {
        List<SecurityPosition> positions = portfolio.getTransactions().stream() //
                        .filter(t -> t.getDate().getTime() <= time.getTime()) //
                        .collect(Collectors.groupingBy(t -> t.getSecurity())) //
                        .entrySet().stream() //
                        .map(e -> new SecurityPosition(e.getKey(), e.getKey().getSecurityPrice(time), e.getValue())) //
                        .filter(p -> p.getShares() != 0) //
                        .collect(Collectors.toList());

        return new PortfolioSnapshot(portfolio, converter, positions);
    }

    public static PortfolioSnapshot merge(List<PortfolioSnapshot> snapshots)
    {
        if (snapshots.isEmpty())
            throw new RuntimeException("Error: PortfolioSnapshots to be merged must not be empty"); //$NON-NLS-1$

        Portfolio portfolio = new Portfolio();
        portfolio.setName(Messages.LabelJointPortfolio);

        Map<Security, SecurityPosition> securities = new HashMap<Security, SecurityPosition>();
        for (PortfolioSnapshot s : snapshots)
        {
            portfolio.addAllTransaction(s.getSource().getTransactions());
            for (SecurityPosition p : s.getPositions())
            {
                SecurityPosition pos = securities.get(p.getSecurity());
                if (pos == null)
                    securities.put(p.getSecurity(), p);
                else
                    securities.put(p.getSecurity(), SecurityPosition.merge(pos, p));
            }
        }

        return new PortfolioSnapshot(portfolio, snapshots.get(0).converter, new ArrayList<SecurityPosition>(
                        securities.values()));
    }

    // //////////////////////////////////////////////////////////////
    // instance impl
    // //////////////////////////////////////////////////////////////

    private final Portfolio portfolio;
    private final CurrencyConverter converter;
    private final List<SecurityPosition> positions;

    private PortfolioSnapshot(Portfolio source, CurrencyConverter converter, List<SecurityPosition> positions)
    {
        this.portfolio = source;
        this.converter = converter;
        this.positions = positions;
    }

    public Portfolio getSource()
    {
        return portfolio;
    }

    public CurrencyConverter getCurrencyConverter()
    {
        return converter;
    }

    public Date getTime()
    {
        return converter.getTime();
    }

    public List<SecurityPosition> getPositions()
    {
        return positions;
    }

    public Map<Security, SecurityPosition> getPositionsBySecurity()
    {
        return positions.stream().collect(Collectors.toMap(SecurityPosition::getSecurity, p -> p));
    }

    public Money getValue()
    {
        return positions.stream() //
                        .map(SecurityPosition::calculateValue) //
                        .map(money -> converter.convert(money)) //
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    public GroupByTaxonomy groupByTaxonomy(Taxonomy taxonomy)
    {
        return new GroupByTaxonomy(taxonomy, this);
    }
}
