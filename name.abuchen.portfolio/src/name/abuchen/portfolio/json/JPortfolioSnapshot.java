package name.abuchen.portfolio.json;

import java.util.List;
import java.util.stream.Collectors;

import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;

public class JPortfolioSnapshot
{
    private String portfolioId;
    private List<JSecurityPosition> positions;

    public List<JSecurityPosition> getPositions()
    {
        return positions;
    }
    
    public String getPortfolioId() 
    {
        return portfolioId;
    }
    
    public static JPortfolioSnapshot from(PortfolioSnapshot snapshot, SecurityPerformanceSnapshot performanceSnapshot)
    {
        JPortfolioSnapshot s = new JPortfolioSnapshot();
        s.positions = snapshot.getPositions().stream().map(position -> {
            var p = JSecurityPosition.from(position);
            var o = performanceSnapshot.getRecord(position.getSecurity());
            if(o.isPresent()) 
            {
                p.setRecord(JSecurityPerformanceRecord.from(o.get()));
            }
            
            return p;
        }).collect(Collectors.toList());
        
        s.portfolioId = snapshot.getPortfolio().getUUID();
        
        return s;
    }

}
