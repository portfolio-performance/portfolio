package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.util.OnlineHelper;
import name.abuchen.portfolio.util.WebAccess;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

/* package */ class YahooSymbolSearch
{
    /* package */ static class Result implements ResultItem
    {
        private String symbol;
        private String name;
        private String type;
        private String exchange;

        public static Optional<Result> from(JSONObject json)
        {
            var symbol = (String) json.get("symbol"); //$NON-NLS-1$
            if (symbol == null)
                return Optional.empty();

            var name = (String) json.get("name"); //$NON-NLS-1$
            if (name == null)
                name = (String) json.get("longname"); //$NON-NLS-1$
            if (name == null)
                name = (String) json.get("shortname"); //$NON-NLS-1$
            if (name == null)
                name = (String) json.get("shortName"); //$NON-NLS-1$

            var type = (String) json.get("typeDisp"); //$NON-NLS-1$
            if (type == null)
                type = (String) json.get("quoteType"); //$NON-NLS-1$

            // Convert the security type using the SecuritySearchProvider
            // instance
            type = SecuritySearchProvider.convertType(type);

            var exchange = (String) json.get("exchDisp"); //$NON-NLS-1$
            if (exchange == null)
                exchange = (String) json.get("exchange"); //$NON-NLS-1$

            return Optional.of(new Result(symbol, name, type, exchange));
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
        public String getSource()
        {
            return Messages.LabelYahooFinance;
        }

        @Override
        public String getFeedId()
        {
            return YahooFinanceQuoteFeed.ID;
        }

        @Override
        public Security create(Client client)
        {
            var security = new Security(name, client.getBaseCurrency());
            security.setTickerSymbol(symbol);
            security.setFeed(YahooFinanceQuoteFeed.ID);
            return security;
        }
    }

    public Stream<Result> search(String query) throws IOException
    {
        List<Result> answer = new ArrayList<>();

        try
        {
            @SuppressWarnings("nls")
            var html = new WebAccess("query2.finance.yahoo.com", "/v1/finance/search") //
                            .addUserAgent(OnlineHelper.getYahooFinanceUserAgent()) //
                            .addParameter("q", query) //
                            .addParameter("region", "DE") //
                            .addParameter("lang", "de-DE") //
                            .addParameter("quotesCount", "6") //
                            .addParameter("newsCount", "0") //
                            .addParameter("enableFuzzyQuery", "false") //
                            .addParameter("quotesQueryId", "tss_match_phrase_query") //
                            .addParameter("multiQuoteQueryId", "multi_quote_single_token_query") //
                            .addParameter("newsQueryId", "news_cie_vespa") //
                            .addParameter("enableCb", "false") //
                            .addParameter("enableNavLinks", "false") //
                            .addParameter("enableEnhancedTrivialQuery", "false") //
                            .get();

            var responseData = (JSONObject) JSONValue.parse(html);
            if (responseData != null)
            {
                var result = (JSONArray) responseData.get("quotes"); //$NON-NLS-1$
                if (result != null)
                {
                    for (var ii = 0; ii < result.size(); ii++)
                        Result.from((JSONObject) result.get(ii)).ifPresent(answer::add);
                }
            }
        }
        catch (WebAccessException ex)
        {
            PortfolioLog.error(ex);
        }

        return answer.stream();
    }
}
