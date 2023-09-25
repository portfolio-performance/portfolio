package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import com.google.common.base.Strings;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceIndicator;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.WebAccess;

public class DivvyDiaryUploader
{
    public static record DDPortfolio(long id, String name)
    {
    }

    private String apiKey;

    public DivvyDiaryUploader(String apiKey)
    {
        this.apiKey = Objects.requireNonNull(apiKey);
    }

    public List<DDPortfolio> getPortfolios() throws IOException
    {
        List<DDPortfolio> answer = new ArrayList<>();

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
                answer.add(new DDPortfolio((Long) portfolio.get("id"), (String) portfolio.get("name"))); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return answer;
    }

    @SuppressWarnings({ "unchecked", "nls" })
    public void upload(Client client, CurrencyConverter converter, long portfolioId, boolean includeTransactions)
                    throws IOException
    {
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, LocalDate.now());
        PortfolioSnapshot portfolio = snapshot.getJointPortfolio();

        SecurityPerformanceSnapshot performance = SecurityPerformanceSnapshot.create(client, converter,
                        Interval.of(LocalDate.MIN, LocalDate.now()), SecurityPerformanceIndicator.Costs.class);

        JSONArray securities = new JSONArray();
        JSONArray activities = new JSONArray();

        for (SecurityPosition position : portfolio.getPositions()) // NOSONAR
        {
            if (!(position.getInvestmentVehicle() instanceof Security))
                continue;

            Security security = position.getSecurity();
            if (Strings.isNullOrEmpty(security.getIsin()))
                continue;

            JSONObject item = new JSONObject();
            item.put("isin", security.getIsin());
            item.put("quantity", position.getShares() / Values.Share.divider());

            // traditional "snapshot" of current holding as "buyin"
            performance.getRecord(security).ifPresent(r -> {
                Quote fifo = r.getFifoCostPerSharesHeld();
                JSONObject buyin = new JSONObject();
                buyin.put("price", fifo.getAmount() / Values.Quote.divider());
                buyin.put("currency", fifo.getCurrencyCode());
                item.put("buyin", buyin);
            });

            securities.add(item);
            if (includeTransactions)
            {
                for (TransactionPair<?> pair : security.getTransactions(client)) // NOSONAR
                {
                    if (!(pair.getTransaction() instanceof PortfolioTransaction))
                        continue;

                    PortfolioTransaction tx = (PortfolioTransaction) pair.getTransaction();
                    if (tx.getType() == PortfolioTransaction.Type.TRANSFER_IN
                                    || tx.getType() == PortfolioTransaction.Type.TRANSFER_OUT)
                        continue;

                    JSONObject activity = new JSONObject();
                    activity.put("type", tx.getType().isPurchase() ? "BUY" : "SELL");
                    activity.put("isin", security.getIsin());

                    LocalDateTime datetime = tx.getDateTime();
                    if (datetime.getHour() == 0 && datetime.getMinute() == 0)
                        datetime = datetime.withHour(12);
                    activity.put("datetime",
                                    datetime.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT));

                    activity.put("quantity", tx.getShares() / Values.Share.divider());
                    activity.put("amount", tx.getGrossValue().getAmount() / Values.Amount.divider());
                    activity.put("fees", tx.getUnitSum(Unit.Type.FEE).getAmount() / Values.Amount.divider());
                    activity.put("taxes", tx.getUnitSum(Unit.Type.TAX).getAmount() / Values.Amount.divider());
                    activity.put("currency", tx.getCurrencyCode());
                    activity.put("broker", "portfolioperformance");
                    activity.put("brokerReference", tx.getUUID());

                    activities.add(activity);
                }
            }
        }

        if (securities.isEmpty())
            return;

        WebAccess upload = new WebAccess("api.divvydiary.com", "/portfolios/" + portfolioId + "/import"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        upload.addHeader("X-API-Key", apiKey); //$NON-NLS-1$
        upload.addHeader("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$

        // inform DivvyDiary that transactions are split adjusted
        upload.addParameter("splitAdjusted", "true");

        JSONObject json = new JSONObject();

        json.put("securities", securities);
        if (!activities.isEmpty())
            json.put("activities", activities);

        upload.post(JSONValue.toJSONString(json));
    }

}
