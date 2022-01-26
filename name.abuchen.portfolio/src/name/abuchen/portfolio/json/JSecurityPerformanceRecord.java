package name.abuchen.portfolio.json;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;

public class JSecurityPerformanceRecord
{
    private Quote fifoCostPerSharesHeld;
    private Money capitalGainsOnHoldings;
    private Quote quote;
    private Money marketValue;
    
    public static JSecurityPerformanceRecord from(SecurityPerformanceRecord record)
    {
        JSecurityPerformanceRecord s = new JSecurityPerformanceRecord();
        s.fifoCostPerSharesHeld = record.getFifoCostPerSharesHeld();
        s.capitalGainsOnHoldings = record.getCapitalGainsOnHoldings();
        s.quote = record.getQuote();
        s.marketValue = record.getMarketValue();        
                
        return s;
    }

    public Quote getFifoCostPerSharesHeld()
    {
        return fifoCostPerSharesHeld;
    }

    public Money getCapitalGainsOnHoldings()
    {
        return capitalGainsOnHoldings;
    }

    public Quote getQuote()
    {
        return quote;
    }

    public Money getMarketValue()
    {
        return marketValue;
    }
}
