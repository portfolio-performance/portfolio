package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.List;

import name.abuchen.portfolio.online.impl.YahooSearchProvider;

public interface SecuritySearchProvider
{
    public static class ResultItem
    {
        private String symbol;
        private String name;
        private String isin;
        private int lastTrade;
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

        public String getIsin()
        {
            return isin;
        }

        public void setIsin(String isin)
        {
            this.isin = isin;
        }

        public int getLastTrade()
        {
            return lastTrade;
        }

        public void setLastTrade(int lastTrade)
        {
            this.lastTrade = lastTrade;
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

    }

    String getName();

    List<ResultItem> search(String query) throws IOException;

    public static final SecuritySearchProvider INSTANCE = new YahooSearchProvider();
}
