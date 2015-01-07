package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
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
        CurrencyConverter converter = new CurrencyConverterImpl(null, CurrencyUnit.EUR);
        return create(portfolio, converter, time);
    }

    public static PortfolioSnapshot create(Portfolio portfolio, CurrencyConverter converter, Date date)
    {
        List<SecurityPosition> positions = portfolio
                        .getTransactions()
                        .stream()
                        .filter(t -> t.getDate().getTime() <= date.getTime())
                        .collect(Collectors.groupingBy(t -> t.getSecurity()))
                        .entrySet()
                        .stream()
                        .map(e -> new SecurityPosition(e.getKey(), converter, e.getKey().getSecurityPrice(date), e
                                        .getValue())) //
                        .filter(p -> p.getShares() != 0) //
                        .collect(Collectors.toList());

        return new PortfolioSnapshot(portfolio, converter, date, positions);
    }

    public static PortfolioSnapshot merge(List<PortfolioSnapshot> snapshots, CurrencyConverter converter)
    {
        if (snapshots.isEmpty())
            throw new RuntimeException("Error: PortfolioSnapshots to be merged must not be empty"); //$NON-NLS-1$

        Portfolio portfolio = new Portfolio()
        {
            @Override
            public List<PortfolioTransaction> getTransactions()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addTransaction(PortfolioTransaction transaction)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addAllTransaction(List<PortfolioTransaction> transactions)
            {
                throw new UnsupportedOperationException();
            }
        };
        portfolio.setName(Messages.LabelJointPortfolio);
        Account referenceAccount = new Account(Messages.LabelJointPortfolio);
        referenceAccount.setCurrencyCode(converter.getTermCurrency());
        portfolio.setReferenceAccount(referenceAccount);

        Map<Security, SecurityPosition> securities = new HashMap<Security, SecurityPosition>();
        for (PortfolioSnapshot snapshot : snapshots)
        {
            for (SecurityPosition position : snapshot.getPositions())
            {
                SecurityPosition existing = securities.get(position.getSecurity());
                if (existing == null)
                    securities.put(position.getSecurity(), position);
                else
                    securities.put(position.getSecurity(), SecurityPosition.merge(existing, position));
            }
        }

        return new PortfolioSnapshot(portfolio, snapshots.get(0).getCurrencyConverter(), snapshots.get(0).getTime(),
                        new ArrayList<SecurityPosition>(securities.values()));
    }

    // //////////////////////////////////////////////////////////////
    // instance impl
    // //////////////////////////////////////////////////////////////

    private final Portfolio portfolio;
    private final CurrencyConverter converter;
    private final Date date;
    private final List<SecurityPosition> positions;

    private PortfolioSnapshot(Portfolio source, CurrencyConverter converter, Date date, List<SecurityPosition> positions)
    {
        this.portfolio = source;
        this.converter = converter;
        this.date = date;
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
        return date;
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
                        .map(money -> converter.convert(date, money)) //
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    public GroupByTaxonomy groupByTaxonomy(Taxonomy taxonomy)
    {
        return new GroupByTaxonomy(taxonomy, this);
    }
}
