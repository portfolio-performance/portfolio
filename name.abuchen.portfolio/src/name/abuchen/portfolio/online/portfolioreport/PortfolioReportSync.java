package name.abuchen.portfolio.online.portfolioreport;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class PortfolioReportSync
{
    private static final String PORTFOLIO_ID_KEY = "net.portfolio-report.portfolioId"; //$NON-NLS-1$

    private final Client client;
    private final PRApiClient api;

    public PortfolioReportSync(String apiKey, Client client)
    {
        this.client = client;

        this.api = new PRApiClient(apiKey);
    }

    public void sync() throws IOException
    {
        long portfolioId = getOrCreatePortfolio();

        PortfolioLog.warning("Syncing with " + portfolioId);

        syncSecurities(portfolioId);

    }

    private long getOrCreatePortfolio() throws IOException
    {
        PRPortfolio remote = null;

        String portfolioId = client.getProperty(PORTFOLIO_ID_KEY);
        if (portfolioId != null)
        {
            try
            {
                long id = Long.parseLong(portfolioId);

                List<PRPortfolio> portfolios = api.listPortfolios();
                remote = portfolios.stream().filter(p -> p.getId() == id).findAny().orElse(null);
            }
            catch (NumberFormatException e)
            {
                // ignore - create new remote portfolio
            }
        }

        if (remote == null)
        {
            PRPortfolio newPortfolio = new PRPortfolio();
            newPortfolio.setName("Synced Portfolio");
            newPortfolio.setNote("automatically created by PP");
            newPortfolio.setBaseCurrencyCode(client.getBaseCurrency());

            remote = api.createPortfolio(newPortfolio);

            client.setProperty(PORTFOLIO_ID_KEY, String.valueOf(remote.getId()));
        }

        return remote.getId();
    }

    private void syncSecurities(long portfolioId) throws IOException
    {
        List<PRSecurity> securities = api.listSecurities(portfolioId);

        Map<String, PRSecurity> uuid2security = securities.stream().collect(Collectors.toMap(s -> s.getUuid(), s -> s));

        for (Security local : client.getSecurities())
        {
            // indeces without currency not supported remotely
            if (local.getCurrencyCode() == null)
                continue;

            PRSecurity remote = uuid2security.remove(local.getUUID());

            if (remote == null)
            {
                remote = new PRSecurity();
                remote.setUuid(local.getUUID());
                remote.setName(local.getName());
                remote.setCurrencyCode(local.getCurrencyCode());
                remote.setIsin(local.getIsin());
                remote.setWkn(local.getWkn() != null ? local.getWkn() : ""); //$NON-NLS-1$
                remote.setSymbol(local.getTickerSymbol() != null ? local.getTickerSymbol() : ""); //$NON-NLS-1$
                remote.setActive(!local.isRetired());
                remote.setNote(local.getNote() != null ? local.getNote() : ""); //$NON-NLS-1$

                api.createSecurity(portfolioId, remote);
            }
        }
    }

}
