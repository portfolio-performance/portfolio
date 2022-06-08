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
import name.abuchen.portfolio.util.WebAccess;

public class YahooSearchProvider implements SecuritySearchProvider
{
    @Override
    public String getName()
    {
        return Messages.LabelYahooFinance;
    }

    @Override
    public List<ResultItem> search(String query, Type type) throws IOException
    {
        List<ResultItem> answer = new ArrayList<>();

        // search both the HTML page as well as the symbol search
        addSearchPage(answer, query);
        addSymbolSearchResults(answer, query);

        // filter the search result using the German terms as we search the
        // German Yahoo Finance site

        if (type == Type.SHARE)
            answer = answer.stream().filter(r -> SecuritySearchProvider.Type.SHARE.toString().equals(r.getType()))
                            .collect(Collectors.toList());
        if (type == Type.BOND)
            answer = answer.stream().filter(r -> SecuritySearchProvider.Type.BOND.toString().equals(r.getType()))
                            .collect(Collectors.toList());

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
        JSONObject jsonObject = (JSONObject) JSONValue.parse(html);
        if (jsonObject == null)
            return;

        jsonObject = (JSONObject) jsonObject.get("finance"); //$NON-NLS-1$
        if (jsonObject == null)
            return;

        JSONArray jsonArray = (JSONArray) jsonObject.get("result"); //$NON-NLS-1$
        if (jsonArray == null || jsonArray.isEmpty())
            return;

        jsonObject = (JSONObject) jsonArray.get(0);
        if (jsonObject == null)
            return;

        JSONArray items = (JSONArray) jsonObject.get("documents"); //$NON-NLS-1$
        if (items == null || items.isEmpty())
            return;

        for (int ii = 0; ii < items.size(); ii++)
        {
            JSONObject item = (JSONObject) items.get(ii);
            YahooSymbolSearch.Result.from(item).ifPresent(answer::add);
        }
    }
}
