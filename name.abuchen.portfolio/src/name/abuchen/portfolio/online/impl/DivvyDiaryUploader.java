package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import com.google.common.base.Strings;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.util.WebAccess;

public class DivvyDiaryUploader
{

    @SuppressWarnings("unchecked")
    public void upload(Client client, CurrencyConverter converter, String apiKey) throws IOException
    {
        if (apiKey == null)
            return;

        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, LocalDate.now());
        PortfolioSnapshot portfolio = snapshot.getJointPortfolio();

        List<JSONObject> payload = portfolio.getPositions().stream() //
                        .filter(p -> p.getInvestmentVehicle() instanceof Security)
                        .filter(p -> !Strings.isNullOrEmpty(((Security) p.getInvestmentVehicle()).getIsin())).map(p -> {
                            JSONObject item = new JSONObject();
                            item.put("isin", ((Security) p.getInvestmentVehicle()).getIsin()); //$NON-NLS-1$
                            item.put("quantity", p.getShares() / Values.Share.divider()); //$NON-NLS-1$
                            return item;
                        }) //
                        .collect(Collectors.toList());

        if (payload.isEmpty())
            return;

        String session = new WebAccess("api.divvydiary.com", "/session") //$NON-NLS-1$ //$NON-NLS-2$
                        .addHeader("X-API-Key", apiKey) //$NON-NLS-1$
                        .addUserAgent("PortfolioPerformance/" //$NON-NLS-1$
                                        + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString())
                        .get();

        String userId = String.valueOf(((JSONObject) JSONValue.parse(session)).get("id")); //$NON-NLS-1$
        if (Strings.isNullOrEmpty(userId))
            throw new IOException("DivvyDiary.com userId not found"); //$NON-NLS-1$

        WebAccess upload = new WebAccess("api.divvydiary.com", "/users/" + userId + "/depot/import"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        upload.addHeader("X-API-Key", apiKey); //$NON-NLS-1$
        upload.addHeader("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$

        JSONArray json = new JSONArray();
        json.addAll(payload);

        upload.post(JSONValue.toJSONString(json));
    }

}
