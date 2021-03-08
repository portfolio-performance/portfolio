package name.abuchen.portfolio.online.portfolioreport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;

public class PortfolioReportSync
{
    private static final String PORTFOLIO_ID_KEY = "net.portfolio-report.portfolioId"; //$NON-NLS-1$

    private final Client client;
    private final PRApiClient api;

    Map<String, PRSecurity> remoteSecuritiesByUuid;
    Map<String, PRAccount> remoteAccountsByUuid;

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
        // TODO: syncTransactions(portfolioId);
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
        List<PRSecurity> remoteSecurities = api.listSecurities(portfolioId);

        Map<String, PRSecurity> unmatchedRemoteSecuritiesByUuid = remoteSecurities.stream()
                        .collect(Collectors.toMap(s -> s.getUuid(), s -> s));
        remoteSecuritiesByUuid = remoteSecurities.stream().collect(Collectors.toMap(s -> s.getUuid(), s -> s));

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
    }

    private void syncAccounts(long portfolioId) throws IOException
    {
        List<PRAccount> remoteAccounts = api.listAccounts(portfolioId);

        remoteAccountsByUuid = remoteAccounts.stream().collect(Collectors.toMap(s -> s.getUuid(), s -> s));
        Map<String, PRAccount> unmatchedRemoteAccountsByUuid = remoteAccounts.stream()
                        .collect(Collectors.toMap(s -> s.getUuid(), s -> s));

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
    }

    private PRTransaction convertPortfolioTransaction(PortfolioTransaction pp, Portfolio portfolio)
    {
        PRTransaction pr = new PRTransaction();
        pr.setAccountId(remoteAccountsByUuid.get(portfolio.getUUID()).getId());
        pr.setDatetime(pp.getDateTime());
        pr.setNote(pp.getNote());
        pr.setSecurityId(remoteSecuritiesByUuid.get(pp.getSecurity().getUUID()).getId());
        // TODO: convert amount/units

        if (pp.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND)
        {
            pr.setType("SecuritiesOrder"); //$NON-NLS-1$
            pr.setShares(Double.toString(pp.getShares() / 1e8));
        }
        else if (pp.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
        {
            pr.setType("SecuritiesOrder"); //$NON-NLS-1$
            pr.setShares(Double.toString(-1 * pp.getShares() / 1e8));
        }
        else if (pp.getType() == PortfolioTransaction.Type.BUY)
        {
            pr.setType("SecuritiesOrder"); //$NON-NLS-1$
            pr.setShares(Double.toString(pp.getShares() / 1e8));

            BuySellEntry crossEntry = (BuySellEntry) pp.getCrossEntry();
            pr.setPartnerTransaction(new PRTransaction());
            pr.getPartnerTransaction()
                            .setAccountId(remoteAccountsByUuid.get(crossEntry.getAccount().getUUID()).getId());
        }
        else if (pp.getType() == PortfolioTransaction.Type.SELL)
        {
            pr.setType("SecuritiesOrder"); //$NON-NLS-1$
            pr.setShares(Double.toString(-1 * pp.getShares() / 1e8));

            BuySellEntry crossEntry = (BuySellEntry) pp.getCrossEntry();
            pr.setPartnerTransaction(new PRTransaction());
            pr.getPartnerTransaction()
                            .setAccountId(remoteAccountsByUuid.get(crossEntry.getAccount().getUUID()).getId());
        }
        else if (pp.getType() == PortfolioTransaction.Type.TRANSFER_IN)
        {
            pr.setType("SecuritiesTransfer"); //$NON-NLS-1$
            pr.setShares(Double.toString(pp.getShares() / 1e8));

            PortfolioTransferEntry crossEntry = (PortfolioTransferEntry) pp.getCrossEntry();
            pr.setPartnerTransaction(new PRTransaction());
            pr.getPartnerTransaction()
                            .setAccountId(remoteAccountsByUuid.get(crossEntry.getTargetPortfolio().getUUID()).getId());
        }
        else if (pp.getType() == PortfolioTransaction.Type.TRANSFER_OUT)
        {
            pr.setType("SecuritiesTransfer"); //$NON-NLS-1$
            pr.setShares(Double.toString(-1 * pp.getShares() / 1e8));

            PortfolioTransferEntry crossEntry = (PortfolioTransferEntry) pp.getCrossEntry();
            pr.setPartnerTransaction(new PRTransaction());
            pr.getPartnerTransaction()
                            .setAccountId(remoteAccountsByUuid.get(crossEntry.getSourcePortfolio().getUUID()).getId());
        }
        return pr;
    }

    private void syncTransactions(long portfolioId) throws IOException
    {
        List<PRTransaction> remoteTransactions = api.listTransactions(portfolioId);

        List<PRTransaction> unmatchedRemoteTransactions = new ArrayList<PRTransaction>(remoteTransactions);


        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction pp : portfolio.getTransactions())
            {
                PRTransaction local = convertPortfolioTransaction(pp, portfolio);

                Optional<PRTransaction> remoteMatch = unmatchedRemoteTransactions.stream()
                                .filter(remote -> (local.getAccountId() == remote.getAccountId()
                                                && local.getType() == remote.getType()
                                                && local.getDatetime().equals(remote.getDatetime())
                                                && local.getSecurityId() == remote.getSecurityId()
                                                && local.getShares() == remote.getShares()
                                                && local.getNote() == remote.getNote()))
                                .findFirst();
                // TODO: compare partnerTransaction
                // TODO: compare units

                if (remoteMatch.isPresent())
                {
                    // TODO: remove from unmatchedRemoteTransactions
                }
                else
                {
                    // TODO: create transaction
                }
            }
        }

        // TODO: for (Account account : client.getAccounts()) {}

        // TODO: delete unmatched transactions
    }
}
