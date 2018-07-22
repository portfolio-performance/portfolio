package name.abuchen.portfolio.online.sync;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.OnlineState;
import name.abuchen.portfolio.model.OnlineState.Property;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;

public class PortfolioReportNet
{
    public static class OnlineItem
    {
        private String id;
        private String name;
        private String isin;
        private String wkn;
        private String ticker;

        /* package */ static OnlineItem from(JSONObject json)
        {
            OnlineItem vehicle = new OnlineItem();
            vehicle.id = (String) json.get("uuid"); //$NON-NLS-1$
            vehicle.name = (String) json.get("name"); //$NON-NLS-1$
            vehicle.isin = (String) json.get("isin"); //$NON-NLS-1$
            vehicle.wkn = (String) json.get("wkn"); //$NON-NLS-1$
            vehicle.ticker = (String) json.get("ticker"); //$NON-NLS-1$
            return vehicle;
        }

        private OnlineItem()
        {}

        public String getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }

        public String getIsin()
        {
            return isin;
        }

        public String getWkn()
        {
            return wkn;
        }

        public String getTicker()
        {
            return ticker;
        }

        public ResultItem toResultItem()
        {
            ResultItem resultItem = new ResultItem();

            resultItem.setName(name);
            // resultItem.setIsin(isin); // FIXME isin?
            resultItem.setSymbol(ticker);
            resultItem.setType(Messages.LabelSecurity);

            return resultItem;
        }
    }

    private static final String READ_URL;
    private static final String SEARCH_URL;
    private static final String QUERY_URL;

    static
    {
        String baseURL = System.getProperty("net.portfolio-report.baseUrl"); //$NON-NLS-1$
        if (baseURL == null || baseURL.isEmpty())
            baseURL = "https://api.portfolio-report.net"; //$NON-NLS-1$

        READ_URL = baseURL + "/securities/{0}"; //$NON-NLS-1$
        SEARCH_URL = baseURL + "/securities/search"; //$NON-NLS-1$
        QUERY_URL = baseURL + "/securities/search/{0}"; //$NON-NLS-1$
    }

    public Optional<OnlineItem> getUpdatedValues(Security security) throws IOException
    {
        if (security.getOnlineId() == null)
            throw new IllegalArgumentException();

        String body = buildBody(security);

        String url = MessageFormat.format(READ_URL,
                        URLEncoder.encode(security.getOnlineId(), StandardCharsets.UTF_8.name()));

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setConnectTimeout(1000);
        con.setReadTimeout(2000);

        con.setDoOutput(true);

        con.setRequestProperty("X-Source", "Portfolio Peformance " //$NON-NLS-1$ //$NON-NLS-2$
                        + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString());
        con.setRequestProperty("X-Reason", "periodic update"); //$NON-NLS-1$ //$NON-NLS-2$
        con.setRequestProperty("Content-Type", "application/json;chartset=UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$

        try (OutputStream output = con.getOutputStream())
        {
            output.write(body.getBytes(StandardCharsets.UTF_8));
        }

        Optional<OnlineItem> onlineItem = Optional.empty();

        int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK)
            throw new IOException(url + " --> " + responseCode); //$NON-NLS-1$

        try (Scanner scanner = new Scanner(con.getInputStream(), StandardCharsets.UTF_8.name()))
        {
            String html = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$

            JSONObject response = (JSONObject) JSONValue.parse(html);
            if (response != null)
                onlineItem = Optional.of(OnlineItem.from(response));
        }

        return onlineItem;
    }

    public Optional<OnlineItem> findMatch(Security security) throws IOException
    {
        String body = buildBody(security);

        URL obj = new URL(SEARCH_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setConnectTimeout(1000);
        con.setReadTimeout(2000);

        con.setDoOutput(true);

        con.setRequestProperty("X-Source", "Portfolio Peformance " //$NON-NLS-1$ //$NON-NLS-2$
                        + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString());
        con.setRequestProperty("Content-Type", "application/json;chartset=UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$

        try (OutputStream output = con.getOutputStream())
        {
            output.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK)
            throw new IOException(SEARCH_URL + " --> " + responseCode); //$NON-NLS-1$

        List<OnlineItem> onlineItems = readItems(con);

        // assumption: first in the list is the "best" match
        return onlineItems.isEmpty() ? Optional.empty() : Optional.of(onlineItems.get(0));
    }

    public List<OnlineItem> search(String query) throws IOException
    {
        String searchUrl = MessageFormat.format(QUERY_URL, URLEncoder.encode(query, StandardCharsets.UTF_8.name()));

        URL url = new URL(searchUrl);
        URLConnection con = url.openConnection();
        con.setConnectTimeout(500);
        con.setReadTimeout(2000);

        return readItems(con);
    }

    @SuppressWarnings("unchecked")
    private String buildBody(Security security)
    {
        JSONObject body = new JSONObject();
        for (Property p : OnlineState.Property.values())
        {
            String value = p.getValue(security);
            if (value != null && value.trim().length() > 0)
                body.put(p.name().toLowerCase(Locale.US), value);
        }
        return body.toJSONString();
    }

    private List<OnlineItem> readItems(URLConnection con) throws IOException
    {
        List<OnlineItem> onlineItems = new ArrayList<>();

        try (Scanner scanner = new Scanner(con.getInputStream(), StandardCharsets.UTF_8.name()))
        {
            String html = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$

            JSONArray response = (JSONArray) JSONValue.parse(html);
            if (response != null)
            {
                for (int ii = 0; ii < response.size(); ii++)
                    onlineItems.add(OnlineItem.from((JSONObject) response.get(ii)));
            }
        }

        return onlineItems;
    }
}
