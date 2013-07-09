package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Taxonomy;

public class PortfolioSnapshot
{
    // //////////////////////////////////////////////////////////////
    // factory methods
    // //////////////////////////////////////////////////////////////

    public static PortfolioSnapshot create(Portfolio portfolio, Date time)
    {
        Map<Security, SecurityPosition> positions = new HashMap<Security, SecurityPosition>();

        for (PortfolioTransaction t : portfolio.getTransactions())
        {
            if (t.getDate().getTime() <= time.getTime())
            {
                switch (t.getType())
                {
                    case TRANSFER_IN:
                    case BUY:
                    case DELIVERY_INBOUND:
                    {
                        SecurityPosition p = positions.get(t.getSecurity());
                        if (p == null)
                            positions.put(t.getSecurity(), p = new SecurityPosition(t.getSecurity()));
                        p.addTransaction(t);
                        break;
                    }
                    case TRANSFER_OUT:
                    case SELL:
                    case DELIVERY_OUTBOUND:
                    {
                        SecurityPosition p = positions.get(t.getSecurity());
                        if (p == null)
                            positions.put(t.getSecurity(), p = new SecurityPosition(t.getSecurity()));
                        p.addTransaction(t);
                        break;
                    }
                    default:
                        throw new UnsupportedOperationException("Unsupported operation: " + t.getType()); //$NON-NLS-1$
                }
            }
        }

        ArrayList<SecurityPosition> collection = new ArrayList<SecurityPosition>(positions.values());
        for (Iterator<SecurityPosition> iter = collection.iterator(); iter.hasNext();)
        {
            SecurityPosition p = iter.next();

            if (p.getShares() == 0)
            {
                iter.remove();
            }
            else
            {
                SecurityPrice price = p.getSecurity().getSecurityPrice(time);
                p.setPrice(price);
            }
        }

        return new PortfolioSnapshot(portfolio, time, collection);
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

        return new PortfolioSnapshot(portfolio, snapshots.get(0).getTime(), new ArrayList<SecurityPosition>(
                        securities.values()));
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
        Map<Security, SecurityPosition> map = new HashMap<Security, SecurityPosition>();
        for (SecurityPosition p : positions)
            map.put(p.getSecurity(), p);
        return map;
    }

    public long getValue()
    {
        long value = 0;
        for (SecurityPosition p : positions)
            value += p.calculateValue();

        return value;
    }

    public GroupByTaxonomy groupByTaxonomy(Taxonomy taxonomy)
    {
        return new GroupByTaxonomy(taxonomy, this);
    }
}
