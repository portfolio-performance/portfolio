package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.WebAccess;

public class MyDividends24Uploader
{

    private final String apiKey;

    public MyDividends24Uploader(String apiKey)
    {
        this.apiKey = Objects.requireNonNull(apiKey, "MyDividende24.de ApiKey must not be null"); //$NON-NLS-1$
    }

    public List<Pair<Integer, String>> getPortfolios() throws IOException
    {
        String response = new WebAccess("dividend-jsa-pp-bnp8.vercel.app", "/api/import/retrieve-depots") //$NON-NLS-1$//$NON-NLS-2$
                        .addHeader("Authorization", "Bearer: " + apiKey).addUserAgent("PortfolioPerformance/" //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
                                        + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString())
                        .get();

        // Parses the response from the API endpoint into a JSONObject
        JSONObject session = (JSONObject) JSONValue.parse(response);

        // Retrieves the list of portfolios from the JSONObject
        @SuppressWarnings("unchecked")
        JSONArray portfolios = (JSONArray) session.getOrDefault("depots", Collections.emptyList()); //$NON-NLS-1$

        // Maps each portfolio to a pair of its index and its name and returns
        // the list of pairs.
        return IntStream.range(0, portfolios.size()).mapToObj(i -> new Pair<>(i, (String) portfolios.get(i))).toList();
    }

    @SuppressWarnings("unchecked")
    public void upload(Client client, CurrencyConverter converter, String portfolioId) throws IOException
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, LocalDate.now());
        PortfolioSnapshot portfolio = snapshot.getJointPortfolio();

        // Filters out any transactions that do not have an ISIN.
        List<JSONObject> resultTransactions = portfolio.getPortfolio().getTransactions().stream()
                        .filter(item -> item.getSecurity().getIsin() != null).map(item -> {
                            double quantity = item.getShares() / Values.Share.divider();
                            double buyingprice = item.getGrossValueAmount() / Values.Amount.divider() / quantity;
                            String purchasedate = item.getDateTime().toString();
                            String isin = item.getSecurity().getIsin();
                            String type = item.getType().isPurchase() ? "buy" : "sell"; //$NON-NLS-1$ //$NON-NLS-2$

                            // Creates a JSONObject for each transaction with
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
        uploadData.put("depot", portfolioId); //$NON-NLS-1$

        System.err.println(JSONValue.toJSONString(uploadData));

        new WebAccess("dividend-jsa-pp-bnp8.vercel.app", "/api/import/import") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Authorization", "Bearer: " + apiKey) // //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Content-Type", "application/json") //$NON-NLS-1$//$NON-NLS-2$
                        .post(JSONValue.toJSONString(uploadData));
    }
}
