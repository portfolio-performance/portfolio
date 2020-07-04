package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.List;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendPayment;

public interface DividendFeed
{

    public List<DividendPayment> getDividendPayments(Security security) throws IOException;

}
