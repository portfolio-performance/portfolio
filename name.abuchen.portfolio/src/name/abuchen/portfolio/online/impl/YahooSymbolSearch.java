package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.util.WebAccess;

/* package */ class YahooSymbolSearch
{
    /* package */ static class Result implements ResultItem
    {
        private String symbol;
        private String name;
        private String type;
        private String exchange;

        public static Result from(JSONObject json)
        {
            String symbol = json.get("symbol").toString(); //$NON-NLS-1$
            String name = json.get("name").toString(); //$NON-NLS-1$
            String type = json.get("typeDisp").toString(); //$NON-NLS-1$
            String exchange = json.get("exchDisp").toString(); //$NON-NLS-1$
            return new Result(symbol, name, type, exchange);
        }

        private Result(String symbol, String name, String type, String exchange)
        {
            this.symbol = symbol;
            this.name = name;
            this.type = type;
            this.exchange = exchange;
        }

        /* package */ Result(String name)
        {
            this.name = name;
        }

        @Override
        public String getSymbol()
        {
            return symbol;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public String getType()
        {
            return type;
        }

        @Override
        public String getExchange()
        {
            return exchange;
        }

        @Override
        public String getIsin()
        {
            return null;
        }

        @Override
        public String getWkn()
        {
            return null;
        }

        @Override
        public void applyTo(Security security)
        {
            security.setName(name);
            security.setTickerSymbol(symbol);
            security.setFeed(YahooFinanceQuoteFeed.ID);
        }
    }

    private static final String SEARCH_URL = "/aq/autoc?query={0}&region=DE&lang=de-DE&callback=YAHOO.util.ScriptNodeDataSource.callbacks"; //$NON-NLS-1$

    public Stream<Result> search(String query) throws IOException
    {
        List<Result> answer = new ArrayList<>();

        String html = new WebAccess().document("https", "s.yimg.com", SEARCH_URL) //$NON-NLS-1$ //$NON-NLS-2$
                        .get();
        // strip away java script call back method
        int start = html.indexOf('(');
        int end = html.lastIndexOf(')');
        html = html.substring(start + 1, end);

        JSONObject responseData = (JSONObject) JSONValue.parse(html);
        if (responseData != null)
        {
            JSONObject resultSet = (JSONObject) responseData.get("ResultSet"); //$NON-NLS-1$
            if (resultSet != null)
            {
                JSONArray result = (JSONArray) resultSet.get("Result"); //$NON-NLS-1$
                if (result != null)
                {
                    for (int ii = 0; ii < result.size(); ii++)
                        answer.add(Result.from((JSONObject) result.get(ii)));
                }
            }
        }

        return answer.stream();
    }
}
