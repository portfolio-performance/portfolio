package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import com.google.common.base.Strings;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.DividendFeed;
import name.abuchen.portfolio.util.WebAccess;

public class DivvyDiaryDividendFeed implements DividendFeed
{
    private String apiKey;

    private PageCache<String> cache = new PageCache<>();

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DividendEvent> getDividendPayments(Security security) throws IOException
    {
        if (apiKey == null)
            return Collections.emptyList();

        if (Strings.isNullOrEmpty(security.getIsin()))
            return Collections.emptyList();

        String json = cache.lookup(security.getIsin());
        if (json == null)
        {
            json = new WebAccess("api.divvydiary.com", "/symbols/" + security.getIsin()) //$NON-NLS-1$ //$NON-NLS-2$
                            .addHeader("X-API-Key", apiKey) //$NON-NLS-1$
                            .addUserAgent("PortfolioPerformance/" //$NON-NLS-1$
                                            + FrameworkUtil.getBundle(PortfolioReportNet.class).getVersion().toString())
                            .get();
            cache.put(security.getIsin(), json);
        }

        JSONObject jsonObject = (JSONObject) JSONValue.parse(json);

        JSONArray dividends = (JSONArray) jsonObject.get("dividends"); //$NON-NLS-1$

        List<DividendEvent> answer = new ArrayList<>();

        dividends.forEach(entry -> {
            JSONObject row = (JSONObject) entry;

            DividendEvent payment = new DividendEvent();

            payment.setDate(YahooHelper.fromISODate((String) row.get("exDate"))); //$NON-NLS-1$
            payment.setPaymentDate(YahooHelper.fromISODate((String) row.get("payDate"))); //$NON-NLS-1$
            payment.setAmount(Money.of((String) row.get("currency"), //$NON-NLS-1$
                            Values.Amount.factorize(((Number) row.get("amount")).doubleValue()))); //$NON-NLS-1$

            payment.setSource("divvydiary.com"); //$NON-NLS-1$

            answer.add(payment);
        });

        return answer;
    }

}
