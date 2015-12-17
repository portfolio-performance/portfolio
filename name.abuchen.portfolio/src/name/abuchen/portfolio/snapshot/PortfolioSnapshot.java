package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

public class PortfolioSnapshot
{
    // //////////////////////////////////////////////////////////////
    // factory methods
    // //////////////////////////////////////////////////////////////

    public static PortfolioSnapshot create(Portfolio portfolio, Date time)
    {
        List<SecurityPosition> positions = portfolio.getTransactions().stream() //
                        .filter(t -> t.getDate().getTime() <= time.getTime()) //
                        .collect(Collectors.groupingBy(t -> t.getSecurity())) //
                        .entrySet().stream() //
                        .map(e -> new SecurityPosition(e.getKey(), e.getKey().getSecurityPrice(time), e.getValue())) //
                        .filter(p -> p.getShares() != 0) //
                        .collect(Collectors.toList());

        return new PortfolioSnapshot(portfolio, time, positions);
    }

    public static PortfolioSnapshot merge(List<PortfolioSnapshot> snapshots)
    {
        if (snapshots.isEmpty())
            throw new RuntimeException("Error: PortfolioSnapshots to be merged must not be empty"); //$NON-NLS-1$

        Portfolio portfolio = new Portfolio();
        portfolio.setName(Messages.LabelJointPortfolio);

        snapshots.forEach(s -> portfolio.addAllTransaction(s.getSource().getTransactions()));

        return create(portfolio, snapshots.get(0).getTime());
    }

    // //////////////////////////////////////////////////////////////
    // instance impl
    // //////////////////////////////////////////////////////////////

    private Portfolio portfolio;
    private Date time;

    private List<SecurityPosition> positions = new ArrayList<SecurityPosition>();

    private PortfolioSnapshot(Portfolio source, Date time, List<SecurityPosition> positions)
    {
        this.portfolio = source;
        this.time = time;
        this.positions = positions;
    }

    public Portfolio getSource()
    {
        return portfolio;
    }

    public Date getTime()
    {
        return time;
    }

    public List<SecurityPosition> getPositions()
    {
        return positions;
    }

    public Map<Security, SecurityPosition> getPositionsBySecurity()
    {
        return positions.stream().collect(Collectors.toMap(SecurityPosition::getSecurity, p -> p));
    }

    public long getValue()
    {
        return positions.stream().mapToLong(p -> p.calculateValue()).sum();
    }

    public GroupByTaxonomy groupByTaxonomy(Taxonomy taxonomy)
    {
        return new GroupByTaxonomy(taxonomy, this);
    }
}
