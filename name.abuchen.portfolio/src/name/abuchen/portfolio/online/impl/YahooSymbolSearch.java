package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/* package */ class YahooSymbolSearch
{
    /* package */ static class Result
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

        public String getSymbol()
        {
            return symbol;
        }

        public String getName()
        {
            return name;
        }

        public String getType()
        {
            return type;
        }

        public String getExchange()
        {
            return exchange;
        }
    }

    private static final String SEARCH_URL = "https://s.yimg.com/aq/autoc?query={0}&region=DE&lang=de-DE&callback=YAHOO.util.ScriptNodeDataSource.callbacks"; //$NON-NLS-1$

    public Stream<Result> search(String query) throws IOException
    {
        List<Result> answer = new ArrayList<>();
        
        try (CloseableHttpClient client = HttpClients.createDefault())
        {
            // http://stackoverflow.com/questions/885456/stock-ticker-symbol-lookup-api
            String searchUrl = MessageFormat.format(SEARCH_URL,
                            URLEncoder.encode(query, StandardCharsets.UTF_8.name()));

            try (CloseableHttpResponse response = client.execute(new HttpGet(searchUrl)))
            {
                String html = EntityUtils.toString(response.getEntity());

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
            }
        }
        
        return answer.stream();
    }
}
