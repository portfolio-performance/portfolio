package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.List;

import name.abuchen.portfolio.model.Security;

public interface SecuritySearchProvider
{
    public static class ResultItem
    {
        private String symbol;
        private String name;
        private String type;
        private String exchange;

        public String getSymbol()
        {
            return symbol;
        }

        public void setSymbol(String symbol)
        {
            this.symbol = symbol;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }

        public String getExchange()
        {
            return exchange;
        }

        public void setExchange(String exchange)
        {
            this.exchange = exchange;
        }

        public void applyTo(Security security)
        {
            security.setTickerSymbol(getSymbol());
            security.setName(getName());
        }
    }

    String getName();

    List<ResultItem> search(String query) throws IOException;
}
