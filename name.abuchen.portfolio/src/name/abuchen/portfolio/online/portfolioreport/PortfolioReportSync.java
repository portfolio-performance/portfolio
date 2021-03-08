package name.abuchen.portfolio.online.portfolioreport;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
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
        syncAccounts(portfolioId);
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

    private Map<String, PRSecurity> syncSecurities(long portfolioId) throws IOException
    {
        List<PRSecurity> remoteSecurities = api.listSecurities(portfolioId);

        Map<String, PRSecurity> unmatchedRemoteSecuritiesByUuid = remoteSecurities.stream().collect(Collectors.toMap(s -> s.getUuid(), s -> s));
        Map<String, PRSecurity> remoteSecuritiesByUuid = remoteSecurities.stream().collect(Collectors.toMap(s -> s.getUuid(), s -> s));

        for (Security local : client.getSecurities())
        {
            // indeces without currency not supported remotely
            if (local.getCurrencyCode() == null)
                continue;

            PRSecurity remote = unmatchedRemoteSecuritiesByUuid.remove(local.getUUID());

            if (remote == null)
            {
                // Create remote security
                remote = new PRSecurity();
                remote.setUuid(local.getUUID());
                remote.setName(local.getName());
                remote.setCurrencyCode(local.getCurrencyCode());
                remote.setIsin(local.getIsin());
                remote.setWkn(local.getWkn() != null ? local.getWkn() : ""); //$NON-NLS-1$
                remote.setSymbol(local.getTickerSymbol() != null ? local.getTickerSymbol() : ""); //$NON-NLS-1$
                remote.setActive(!local.isRetired());
                remote.setNote(local.getNote() != null ? local.getNote() : ""); //$NON-NLS-1$

                PRSecurity createdSecurity = api.createSecurity(portfolioId, remote);
                remoteSecuritiesByUuid.put(createdSecurity.getUuid(), createdSecurity);
            }
            else
            {
                // Update remote security
                remote.setName(local.getName());
                remote.setCurrencyCode(local.getCurrencyCode());
                remote.setIsin(local.getIsin());
                remote.setWkn(local.getWkn() != null ? local.getWkn() : ""); //$NON-NLS-1$
                remote.setSymbol(local.getTickerSymbol() != null ? local.getTickerSymbol() : ""); //$NON-NLS-1$
                remote.setActive(!local.isRetired());
                remote.setNote(local.getNote() != null ? local.getNote() : ""); //$NON-NLS-1$
                
                api.updateSecurity(portfolioId, remote);
            }
        }
        
        // Delete unmatched remote securities
        for (PRSecurity security : unmatchedRemoteSecuritiesByUuid.values())
        {
            api.deleteSecurity(portfolioId, security);
        }

        return remoteSecuritiesByUuid;
    }

    private Map<String, PRAccount> syncAccounts(long portfolioId) throws IOException
    {
        List<PRAccount> remoteAccounts = api.listAccounts(portfolioId);

        Map<String, PRAccount> remoteAccountsByUuid = remoteAccounts.stream().collect(Collectors.toMap(s -> s.getUuid(), s -> s));
        Map<String, PRAccount> unmatchedRemoteAccountsByUuid = remoteAccounts.stream().collect(Collectors.toMap(s -> s.getUuid(), s -> s));

        for (Account local : client.getAccounts())
        {
            PRAccount remote = unmatchedRemoteAccountsByUuid.remove(local.getUUID());
            
            if (remote == null)
            {
                // Create remote account
                remote = new PRAccount();
                remote.setType("deposit"); //$NON-NLS-1$
                remote.setUuid(local.getUUID());
                remote.setName(local.getName());
                remote.setNote(local.getNote() != null ? local.getNote() : ""); //$NON-NLS-1$
                remote.setActive(!local.isRetired());
                
                remote.setCurrencyCode(local.getCurrencyCode());
                
                PRAccount createdAccount = api.createAccount(portfolioId, remote);
                remoteAccountsByUuid.put(createdAccount.getUuid(), createdAccount);
            }
            else
            {
                // Update remote account
                remote.setName(local.getName());
                remote.setNote(local.getNote() != null ? local.getNote() : ""); //$NON-NLS-1$
                remote.setActive(!local.isRetired());
                
                remote.setCurrencyCode(local.getCurrencyCode());
                
                api.updateAccount(portfolioId, remote);
            }
        }
        
        for (Portfolio local : client.getPortfolios())
        {
            PRAccount remote = unmatchedRemoteAccountsByUuid.remove(local.getUUID());
            
            if (remote == null)
            {
                // Create remote account 
                remote = new PRAccount();
                remote.setType("securities"); //$NON-NLS-1$
                remote.setUuid(local.getUUID());
                remote.setName(local.getName());
                remote.setNote(local.getNote() != null ? local.getNote() : ""); //$NON-NLS-1$
                remote.setActive(!local.isRetired());
                
                String referenceAccountUuid = local.getReferenceAccount().getUUID();
                long referenceAccountId = remoteAccountsByUuid.get(referenceAccountUuid).getId();
                remote.setReferenceAccountId(referenceAccountId);
                
                api.createAccount(portfolioId, remote);
            }
            else
            {
                // Update remote account
                remote.setName(local.getName());
                remote.setNote(local.getNote() != null ? local.getNote() : ""); //$NON-NLS-1$
                remote.setActive(!local.isRetired());
                
                String referenceAccountUuid = local.getReferenceAccount().getUUID();
                long referenceAccountId = remoteAccountsByUuid.get(referenceAccountUuid).getId();
                remote.setReferenceAccountId(referenceAccountId);
                
                api.updateAccount(portfolioId, remote);
            }
        }

        // Delete unmatched remote accounts
        for (PRAccount account : unmatchedRemoteAccountsByUuid.values())
        {
            api.deleteAccount(portfolioId, account);
        }
        
        return remoteAccountsByUuid;
    }

}
