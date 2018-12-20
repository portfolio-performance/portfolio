package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.List;

import name.abuchen.portfolio.model.Security;

public interface SecuritySearchProvider
{
    public interface ResultItem
    {
        String getName();

        String getSymbol();
        String getIsin();
        String getWkn();

        String getType();
        String getExchange();

        void applyTo(Security security);
    }

    String getName();

    List<ResultItem> search(String query) throws IOException;
}
