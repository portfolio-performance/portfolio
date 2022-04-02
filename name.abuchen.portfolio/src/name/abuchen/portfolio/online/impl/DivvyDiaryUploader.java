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

import com.google.common.base.Strings;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceIndicator;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.WebAccess;

public class DivvyDiaryUploader
{
    private String apiKey;
    
    public DivvyDiaryUploader(String apiKey)
    {
        this.apiKey = Objects.requireNonNull(apiKey);
    }

    public List<Pair<Long, String>> getPortfolios() throws IOException
    {
        List<Pair<Long, String>> answer = new ArrayList<>();
        
        String response = new WebAccess("api.divvydiary.com", "/session") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("X-API-Key", apiKey) //$NON-NLS-1$
                        .addUserAgent("PortfolioPerformance/" //$NON-NLS-1$
                                        + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString())
                        .get();

        JSONObject session = (JSONObject) JSONValue.parse(response);
        JSONArray portfolios = (JSONArray) session.get("portfolios"); //$NON-NLS-1$
        
        if (portfolios != null)
        {
            for (Object p : portfolios)
            {
                JSONObject portfolio = (JSONObject) p;
                answer.add(new Pair<>((Long)portfolio.get("id"), (String)portfolio.get("name"))); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        
        return answer;
    }

    @SuppressWarnings("unchecked")
    public void upload(Client client, CurrencyConverter converter, long portfolioId) throws IOException
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, LocalDate.now());
        PortfolioSnapshot portfolio = snapshot.getJointPortfolio();

        SecurityPerformanceSnapshot performance = SecurityPerformanceSnapshot.create(client, converter,
                        Interval.of(LocalDate.MIN, LocalDate.now()), SecurityPerformanceIndicator.Costs.class);

        List<JSONObject> payload = portfolio.getPositions().stream() //
                        .filter(p -> p.getInvestmentVehicle() instanceof Security)
                        .filter(p -> !Strings.isNullOrEmpty(((Security) p.getInvestmentVehicle()).getIsin())).map(p -> {
                            JSONObject item = new JSONObject();
                            item.put("isin", ((Security) p.getInvestmentVehicle()).getIsin()); //$NON-NLS-1$
                            item.put("quantity", p.getShares() / Values.Share.divider()); //$NON-NLS-1$

                            performance.getRecord(p.getSecurity()).ifPresent(r -> {

                                Quote fifo = r.getFifoCostPerSharesHeld();

                                JSONObject buyin = new JSONObject();
                                buyin.put("price", fifo.getAmount() / Values.Quote.divider()); //$NON-NLS-1$
                                buyin.put("currency", fifo.getCurrencyCode()); //$NON-NLS-1$
                                item.put("buyin", buyin); //$NON-NLS-1$
                            });

                            return item;
                        }) //
                        .collect(Collectors.toList());

        if (payload.isEmpty())
            return;

        WebAccess upload = new WebAccess("api.divvydiary.com", "/portfolios/" + portfolioId + "/import"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        upload.addHeader("X-API-Key", apiKey); //$NON-NLS-1$
        upload.addHeader("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$

        JSONArray json = new JSONArray();
        json.addAll(payload);

        upload.post(JSONValue.toJSONString(json));
    }

}
