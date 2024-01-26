package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.WebAccess;

public class MyDividends24Uploader
{
    private final String apiKey;

    public MyDividends24Uploader(String apiKey)
    {
        this.apiKey = Objects.requireNonNull(apiKey, "MyDividende24.de ApiKey must not be null"); //$NON-NLS-1$
    }

    @SuppressWarnings("unchecked")
    public List<String> getPortfolios() throws IOException
    {
        String response = new WebAccess("dividend-jsa-pp-bnp8.vercel.app", "/api/import/retrieve-depots") //$NON-NLS-1$//$NON-NLS-2$
                        .addHeader("Authorization", "Bearer: " + apiKey) //$NON-NLS-1$ //$NON-NLS-2$
                        .addUserAgent("PortfolioPerformance/" //$NON-NLS-1$
                                        + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString())
                        .get();

        // Parses the response from the API endpoint into a JSONObject
        JSONObject session = (JSONObject) JSONValue.parse(response);

        // Retrieves the list of portfolios from the JSONObject
        return (List<String>) session.getOrDefault("depots", Collections.<String>emptyList()); //$NON-NLS-1$
    }

    @SuppressWarnings("unchecked")
    public void upload(Client client, String myDividends24PortfolioID, Portfolio portfolio) throws IOException
    {

        Stream<PortfolioTransaction> stream;

        if (portfolio != null)
        {
            // Case: one portfolio of PP is selected
            stream = portfolio.getTransactions().stream();
        }
        else
        {
            // Case: all portfolios are selected

            stream = client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream());
        }

        // Filters out any transactions that do not have an ISIN.
        List<JSONObject> resultTransactions = stream.filter(item -> item.getSecurity().getIsin() != null).map(item -> {
            double quantity = item.getShares() / Values.Share.divider();
            double buyingprice = item.getGrossValueAmount() / Values.Amount.divider() / quantity;
            String purchasedate = item.getDateTime().toString();
            String isin = item.getSecurity().getIsin();
            String type = item.getType().isPurchase() ? "buy" : "sell"; //$NON-NLS-1$ //$NON-NLS-2$

            // Creates a JSONObject for each transaction
            // with
            // the calculated data.
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", type); //$NON-NLS-1$
            jsonObject.put("quantity", quantity); //$NON-NLS-1$
            jsonObject.put("stockprice", buyingprice); //$NON-NLS-1$
            jsonObject.put("date", purchasedate); //$NON-NLS-1$
            jsonObject.put("isin", isin); //$NON-NLS-1$
            return jsonObject;
        }).toList();

        if (resultTransactions.isEmpty())
            return;

        JSONObject uploadData = new JSONObject();
        uploadData.put("transactions", resultTransactions); //$NON-NLS-1$
        uploadData.put("depot", myDividends24PortfolioID); //$NON-NLS-1$

        System.err.println(JSONValue.toJSONString(uploadData));

        new WebAccess("dividend-jsa-pp-bnp8.vercel.app", "/api/import/import") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Authorization", "Bearer: " + apiKey) // //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Content-Type", "application/json") //$NON-NLS-1$//$NON-NLS-2$
                        .post(JSONValue.toJSONString(uploadData));
    }
}
