package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.WebAccess;

public class MyDividends24Uploader
{
    private String apiKey;
    
    public MyDividends24Uploader(String apiKey)
    {
        this.apiKey = Objects.requireNonNull(apiKey);
    }

    public List<Pair<Integer, String>> getPortfolios() throws IOException
    {
        List<Pair<Integer, String>> answer = new ArrayList<>();
        
        String response = new WebAccess("dividend-jsa-pp-bnp8.vercel.app", "/api/import/retrieve-depots") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("Authorization", "Bearer: "+apiKey)//$NON-NLS-1$ //$NON-NLS-2$
                        .addUserAgent("PortfolioPerformance/" //$NON-NLS-1$
                                        + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString())
                        .get();

        JSONObject session = (JSONObject) JSONValue.parse(response);
        JSONArray portfolios = (JSONArray) session.get("depots"); //$NON-NLS-1$
        
               if (portfolios != null)
        {
            int i=0;
            for (Object p : portfolios)
            {
                String portfolio = (String) p;
                answer.add(new Pair<>(i, portfolio));
            }
        }
        
        return answer;
    }

    @SuppressWarnings("unchecked")
    public void upload(Client client, CurrencyConverter converter, String portfolioId) throws IOException
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, LocalDate.now());
        PortfolioSnapshot portfolio = snapshot.getJointPortfolio();
        
        List<JSONObject> resultTransactions = new ArrayList<>();

        List<PortfolioTransaction> transactions = portfolio.getPortfolio().getTransactions().stream().collect(Collectors.toList());

        transactions.forEach( item -> {
            double quantity = item.getShares()/100000000; 
            double buyingprice = item.getGrossValueAmount()/100/quantity;
            String purchasedate = item.getDateTime().toString();
            String isin = item.getSecurity().getIsin();
            String type = "buy"; //$NON-NLS-1$
            if( !item.getType().isPurchase() ) {
                type = "sell"; //$NON-NLS-1$
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", type); //$NON-NLS-1$
            jsonObject.put("quantity", quantity); //$NON-NLS-1$
            jsonObject.put("stockprice", buyingprice); //$NON-NLS-1$
            jsonObject.put("date", purchasedate); //$NON-NLS-1$
            jsonObject.put("isin", isin); //$NON-NLS-1$

            if (isin != null) {
                resultTransactions.add(jsonObject);
                            }
        });

        if (resultTransactions.isEmpty())
            return;

        WebAccess upload = new WebAccess("dividend-jsa-pp-bnp8.vercel.app", "/api/import/import"); //$NON-NLS-1$ //$NON-NLS-2$
        upload.addHeader("Authorization", "Bearer: "+apiKey); //$NON-NLS-1$ //$NON-NLS-2$
        upload.addHeader("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$

        

        JSONObject uploadData = new JSONObject();
        uploadData.put("transactions", resultTransactions); //$NON-NLS-1$
        uploadData.put("depot", portfolioId); //$NON-NLS-1$

        upload.post(JSONValue.toJSONString(uploadData));
        
        
    }

}
