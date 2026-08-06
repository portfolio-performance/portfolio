package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.util.OnlineHelper;
import name.abuchen.portfolio.util.WebAccess;

public class YahooSearchProvider implements SecuritySearchProvider
{
    @Override
    public String getName()
    {
        return Messages.LabelYahooFinance;
    }

    @Override
    public List<ResultItem> search(String query) throws IOException
    {
        List<ResultItem> answer = new ArrayList<>();

        // search both the HTML page as well as the symbol search
        addSearchPage(answer, query);
        addSymbolSearchResults(answer, query);

        if (answer.size() >= 10)
        {
            YahooSymbolSearch.Result item = new YahooSymbolSearch.Result(Messages.MsgMoreResultsAvailable);
            answer.add(item);
        }

        return answer;
    }

    private void addSymbolSearchResults(List<ResultItem> answer, String query) throws IOException
    {
        Set<String> existingSymbols = answer.stream().map(ResultItem::getSymbol).collect(Collectors.toSet());

        new YahooSymbolSearch().search(query)//
                        .filter(r -> !existingSymbols.contains(r.getSymbol())).forEach(answer::add);
    }

    private void addSearchPage(List<ResultItem> answer, String query) throws IOException
    {
        @SuppressWarnings("nls")
        String html = new WebAccess("query2.finance.yahoo.com", "/v1/finance/lookup") //
                        .addUserAgent(OnlineHelper.getYahooFinanceUserAgent()) //
                        .addParameter("formatted", "true") //
                        .addParameter("lang", "de-DE").addParameter("region", "DE") //
                        .addParameter("query", query) //
                        .addParameter("type", "all") //
                        .addParameter("count", "25") //
                        .addParameter("start", "0") //
                        .addParameter("corsDomain", "de.finance.yahoo.com") //
                        .get();

        extractFrom(answer, html);
    }

    /* protected */void extractFrom(List<ResultItem> answer, String html)
    {
        if (!(JSONValue.parse(html) instanceof JSONObject jsonObject))
            return;

        if (!(jsonObject.get("finance") instanceof JSONObject finance)) //$NON-NLS-1$
            return;

        if (!(finance.get("result") instanceof JSONArray results) || results.isEmpty()) //$NON-NLS-1$
            return;

        if (!(results.get(0) instanceof JSONObject firstResult))
            return;

        if (!(firstResult.get("documents") instanceof JSONArray items) || items.isEmpty()) //$NON-NLS-1$
            return;

        for (int ii = 0; ii < items.size(); ii++)
        {
            if (items.get(ii) instanceof JSONObject item)
                YahooSymbolSearch.Result.from(item).ifPresent(answer::add);
        }
    }
}
