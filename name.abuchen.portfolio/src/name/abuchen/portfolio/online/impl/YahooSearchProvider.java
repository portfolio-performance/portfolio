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
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.YahooSymbolSearch.Result;

public class YahooSearchProvider implements SecuritySearchProvider
{
    public static class YahooResultItem extends ResultItem
    {
        @Override
        public void applyTo(Security security)
        {
            super.applyTo(security);
            security.setFeed(YahooFinanceQuoteFeed.ID);
        }

        public static ResultItem from(Result r)
        {
            YahooResultItem item = new YahooResultItem();
            item.setSymbol(r.getSymbol());
            item.setName(r.getName());
            item.setExchange(r.getExchange());
            item.setType(r.getType());
            return item;
        }
    }

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
            ResultItem item = new YahooResultItem();
            item.setName(Messages.MsgMoreResultsAvailable);
            answer.add(item);
        }

        return answer;
    }

    private void addSymbolSearchResults(List<ResultItem> answer, String query) throws IOException
    {
        Set<String> existingSymbols = answer.stream().map(ResultItem::getSymbol).collect(Collectors.toSet());

        new YahooSymbolSearch().search(query)//
                        .filter(r -> !existingSymbols.contains(r.getSymbol()))
                        .forEach(r -> answer.add(YahooResultItem.from(r)));
    }

    private void addSearchPage(List<ResultItem> answer, String query) throws IOException
    {
        try (CloseableHttpClient client = HttpClients.createDefault())
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

                        YahooResultItem resultItem = new YahooResultItem();
                        resultItem.setName(item.get("name").toString()); //$NON-NLS-1$
                        resultItem.setSymbol(item.get("symbol").toString()); //$NON-NLS-1$
                        resultItem.setType(item.get("typeDisp").toString()); //$NON-NLS-1$
                        resultItem.setExchange(item.get("exchDisp").toString()); //$NON-NLS-1$
                        answer.add(resultItem);
                    }
                }
            }
        }
    }
}
