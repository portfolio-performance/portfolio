package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
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
            answer = answer.stream().filter(r -> "Aktie".equals(r.getType())).collect(Collectors.toList()); //$NON-NLS-1$
        if (type == Type.BOND)
            answer = answer.stream().filter(r -> "Anleihe".equals(r.getType())).collect(Collectors.toList()); //$NON-NLS-1$

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
        String templateURL = "/_finance_doubledown/api/resource/searchassist;searchTerm={0}"; //$NON-NLS-1$

        String url = MessageFormat.format(templateURL, URLEncoder.encode(query, StandardCharsets.UTF_8.name()));

        @SuppressWarnings("nls")
        String html = new WebAccess("de.finance.yahoo.com", url) //
                        .addParameter("bkt", "finance-DE-de-DE-def").addParameter("device", "desktop")
                        .addParameter("intl", "de") //
                        .addParameter("lang", "de-DE") //
                        .addParameter("partner", "none") //
                        .addParameter("region", "DE") //
                        .addParameter("site", "finance") //
                        .addParameter("tz", "Europe%2FBerlin") //
                        .addParameter("ver", "0.102.1312") //
                        .addParameter("returnMeta", "true") //
                        .get();

        extractFrom(answer, html);
    }

    /* protected */void extractFrom(List<ResultItem> answer, String html)
    {
        JSONObject response = (JSONObject) JSONValue.parse(html);
        if (response != null)
        {
            JSONObject data = (JSONObject) response.get("data"); //$NON-NLS-1$
            if (data != null)
            {
                JSONArray items = (JSONArray) data.get("items"); //$NON-NLS-1$
                if (items != null)
                {
                    for (int ii = 0; ii < items.size(); ii++)
                    {
                        JSONObject item = (JSONObject) items.get(ii);
                        answer.add(YahooSymbolSearch.Result.from(item));
                    }
                }
            }
        }
    }
}
