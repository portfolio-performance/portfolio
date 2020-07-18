package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.List;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;

public interface DividendFeed
{

    public List<DividendEvent> getDividendPayments(Security security) throws IOException;

}
