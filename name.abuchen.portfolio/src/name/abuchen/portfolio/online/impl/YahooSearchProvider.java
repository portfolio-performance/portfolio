package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.online.SecuritySearchProvider;

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
        try (CloseableHttpClient client = HttpClients.createSystem())
        {
            String templateURL = "https://de.finance.yahoo.com/_finance_doubledown/api/resource/searchassist;" //$NON-NLS-1$
                            + "searchTerm={0}?bkt=finance-DE-de-DE-def&device=desktop&feature=canvassOffnet%2CccOnMute%2CenablePromoImage%2CnewContentAttribution" //$NON-NLS-1$
                            + "%2CrelatedVideoFeature%2CvideoNativePlaylist%2CenableCrypto%2CenableESG%2CenablePrivacyUpdate%2CenableGuceJs%2CenableGuceJsOverlay" //$NON-NLS-1$
                            + "%2CenableCMP%2CenableSingleRail&intl=de&lang=de-DE&partner=none&prid=92ms5apdf6jc3&region=DE&site=finance&tz=Europe%2FBerlin&ver=0.102.1312&returnMeta=true"; //$NON-NLS-1$

            String url = MessageFormat.format(templateURL, URLEncoder.encode(query, StandardCharsets.UTF_8.name()));

            try (CloseableHttpResponse response = client.execute(new HttpGet(url)))
            {
                String body = EntityUtils.toString(response.getEntity());
                extractFrom(answer, body);
            }
        }
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
