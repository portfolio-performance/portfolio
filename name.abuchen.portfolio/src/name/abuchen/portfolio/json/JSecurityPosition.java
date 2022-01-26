package name.abuchen.portfolio.json;

import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.snapshot.SecurityPosition;

public class JSecurityPosition
{
    private Object investment;
    private long shares;
    private SecurityPrice price;
    private JSecurityPerformanceRecord record;
    
    public Object getInvestment()
    {
        return investment;
    }

    public SecurityPrice getPrice()
    {
        return price;
    }
    
    public float getShares()
    {
        return shares;
    }
    
    public static JSecurityPosition from(SecurityPosition position)
    {
        JSecurityPosition s = new JSecurityPosition();
        s.price = position.getPrice();
        s.shares = position.getShares();

        InvestmentVehicle investment = position.getInvestmentVehicle();
        
        if(investment instanceof Security) 
        {        
            s.investment = JSecurity.from((Security) investment);
        }
                
        return s;
    }

    public JSecurityPerformanceRecord getRecord()
    {
        return record;
    }

    public void setRecord(JSecurityPerformanceRecord record)
    {
        this.record = record;
    }


}
