package name.abuchen.portfolio.online.portfolioreport;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;

@SuppressWarnings("nls")
public class PortfolioReportSync
{
    private static final String PORTFOLIO_ID_KEY = "net.portfolio-report.portfolioId";
    private static final String SYNCHRONIZED_AT_KEY = "net.portfolio-report.synchronizedAt";

    private final Client client;
    private final String filename;
    private final PRApiClient api;

    public PortfolioReportSync(String apiKey, Client client, String filename)
    {
        this.client = client;
        this.filename = filename;

        this.api = new PRApiClient(apiKey);
    }

    public void sync(IProgressMonitor monitor) throws IOException
    {
        monitor.beginTask("Syncing with Portfolio Report", 4);

        Instant synchronizedAt = null;
        try
        {
            synchronizedAt = Instant.parse(client.getProperty(SYNCHRONIZED_AT_KEY));
            PortfolioLog.warning("Latest sync: " + synchronizedAt.toString());
        }
        catch (NullPointerException ignore)
        {
            // assume no previous synchronization
        }

        Instant synchronizationStarted = Instant.now();

        long portfolioId = getOrCreatePortfolio();
        monitor.worked(1);

        PortfolioLog.warning(
                        "Syncing with portfolioId " + portfolioId + ", started: " + synchronizationStarted.toString());

        syncSecurities(portfolioId);
        monitor.worked(1);
        syncAccounts(portfolioId);
        monitor.worked(1);
        syncTransactions(portfolioId);
        monitor.worked(1);

        client.setProperty(SYNCHRONIZED_AT_KEY, synchronizationStarted.toString());

        PortfolioLog.warning("Sync finished " + Instant.now().toString());
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
            newPortfolio.setName(filename);
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
                        .collect(Collectors.toMap(PRSecurity::getUuid, s -> s));

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
                remote.setWkn(local.getWkn());
                remote.setSymbol(local.getTickerSymbol());
                remote.setActive(!local.isRetired());
                remote.setNote(local.getNote());
                remote.setSecurityUuid(local.getOnlineId());
                remote.setUpdatedAt(local.getUpdatedAt());

                api.updateSecurity(portfolioId, remote);
            }
            else
            {
                // Update remote security
                remote.setName(local.getName());
                remote.setCurrencyCode(local.getCurrencyCode());
                remote.setIsin(local.getIsin());
                remote.setWkn(local.getWkn());
                remote.setSymbol(local.getTickerSymbol());
                remote.setActive(!local.isRetired());
                remote.setNote(local.getNote());
                remote.setSecurityUuid(local.getOnlineId());
                remote.setUpdatedAt(local.getUpdatedAt());

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

        Map<String, PRAccount> unmatchedRemoteAccountsByUuid = remoteAccounts.stream()
                        .collect(Collectors.toMap(PRAccount::getUuid, s -> s));

        for (Account local : client.getAccounts())
        {
            PRAccount remote = unmatchedRemoteAccountsByUuid.remove(local.getUUID());

            if (remote == null)
            {
                // Create remote account
                remote = new PRAccount();
                remote.setType("deposit");
                remote.setUuid(local.getUUID());
                remote.setName(local.getName());
                remote.setNote(local.getNote());
                remote.setActive(!local.isRetired());
                remote.setCurrencyCode(local.getCurrencyCode());
                remote.setUpdatedAt(local.getUpdatedAt());

                api.updateAccount(portfolioId, remote);
            }
            else
            {
                // Update remote account
                remote.setName(local.getName());
                remote.setNote(local.getNote());
                remote.setActive(!local.isRetired());
                remote.setCurrencyCode(local.getCurrencyCode());
                remote.setUpdatedAt(local.getUpdatedAt());

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
                remote.setType("securities");
                remote.setUuid(local.getUUID());
                remote.setName(local.getName());
                remote.setNote(local.getNote());
                remote.setActive(!local.isRetired());
                remote.setReferenceAccountUuid(local.getReferenceAccount().getUUID());
                remote.setUpdatedAt(local.getUpdatedAt());

                api.updateAccount(portfolioId, remote);
            }
            else
            {
                // Update remote account
                remote.setName(local.getName());
                remote.setNote(local.getNote());
                remote.setActive(!local.isRetired());
                remote.setReferenceAccountUuid(local.getReferenceAccount().getUUID());
                remote.setUpdatedAt(local.getUpdatedAt());

                api.updateAccount(portfolioId, remote);
            }
        }

        // Delete unmatched remote accounts
        for (PRAccount account : unmatchedRemoteAccountsByUuid.values())
        {
            api.deleteAccount(portfolioId, account);
        }
    }

    /**
     * Converts PortfolioTransaction to PRTransaction
     */
    private PRTransaction convertPortfolioTransaction(PortfolioTransaction pp, Portfolio portfolio)
    {
        PRTransaction pr = new PRTransaction();
        pr.setUuid(pp.getUUID());
        pr.setAccountUuid(portfolio.getUUID());
        pr.setDatetime(pp.getDateTime());
        pr.setNote(pp.getNote());
        pr.setPortfolioSecurityUuid(pp.getSecurity().getUUID());
        pr.setUpdatedAt(pp.getUpdatedAt());

        long shares = pp.getShares();
        long amount = pp.getAmount();
        long feeAmount = pp.getUnitSum(Transaction.Unit.Type.FEE).getAmount();
        long taxAmount = pp.getUnitSum(Transaction.Unit.Type.TAX).getAmount();

        PortfolioTransaction.Type type = pp.getType();

        if (type == PortfolioTransaction.Type.DELIVERY_INBOUND)
        {
            pr.setType("SecuritiesOrder");
            pr.setShares(shares);
            pr.addUnit(new PRTransactionUnit("base", amount, pp.getCurrencyCode()));
        }
        else if (type == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
        {
            pr.setType("SecuritiesOrder");
            pr.setShares(-1 * shares);
            pr.addUnit(new PRTransactionUnit("base", -1 * amount, pp.getCurrencyCode()));
        }
        else if (type == PortfolioTransaction.Type.BUY || type == PortfolioTransaction.Type.SELL)
        {
            pr.setType("SecuritiesOrder");
            pr.setPartnerTransactionUuid(((BuySellEntry) pp.getCrossEntry()).getAccountTransaction().getUUID());

            if (type == PortfolioTransaction.Type.BUY)
                amount *= -1;
            else
                shares *= -1;

            pr.setShares(shares);

            if (feeAmount != 0)
            {
                pr.addUnit(new PRTransactionUnit("fee", -1 * feeAmount, pp.getCurrencyCode()));
                amount += feeAmount;
            }

            if (taxAmount != 0)
            {
                pr.addUnit(new PRTransactionUnit("tax", -1 * taxAmount, pp.getCurrencyCode()));
                amount += taxAmount;
            }
            pr.addUnit(new PRTransactionUnit("base", amount, pp.getCurrencyCode()));
        }
        else if (type == PortfolioTransaction.Type.TRANSFER_IN)
        {
            pr.setType("SecuritiesTransfer");
            pr.setShares(shares);
            pr.setPartnerTransactionUuid(
                            ((PortfolioTransferEntry) pp.getCrossEntry()).getTargetTransaction().getUUID());
            pr.addUnit(new PRTransactionUnit("base", -1 * amount, pp.getCurrencyCode()));
        }
        else if (type == PortfolioTransaction.Type.TRANSFER_OUT)
        {
            pr.setType("SecuritiesTransfer");
            pr.setShares(-1 * shares);
            pr.setPartnerTransactionUuid(
                            ((PortfolioTransferEntry) pp.getCrossEntry()).getSourceTransaction().getUUID());
            pr.addUnit(new PRTransactionUnit("base", amount, pp.getCurrencyCode()));
        }
        return pr;
    }

    /**
     * Converts AccountTransaction to PRTransactions. Usually the list will
     * contain one item, for some AccountTransactions an additional
     * PRTransactions is returned which is assigned to the security.
     */
    private List<PRTransaction> convertAccountTransaction(AccountTransaction pp, Account account,
                    String remotePartnerTransactionUuid)
    {
        List<PRTransaction> ret = new ArrayList<>();
        PRTransaction pr = new PRTransaction();
        ret.add(pr);

        pr.setUuid(pp.getUUID());
        pr.setAccountUuid(account.getUUID());
        pr.setDatetime(pp.getDateTime());
        pr.setNote(pp.getNote());
        pr.setUpdatedAt(pp.getUpdatedAt());

        // Prepare additional transaction in case we need it later
        PRTransaction pr2 = new PRTransaction(pr);
        pr2.setUuid(remotePartnerTransactionUuid != null ? remotePartnerTransactionUuid : UUID.randomUUID().toString());
        pr2.setPartnerTransactionUuid(pr.getUuid());

        long amount = pp.getAmount();
        long feeAmount = pp.getUnitSum(Transaction.Unit.Type.FEE).getAmount();
        long taxAmount = pp.getUnitSum(Transaction.Unit.Type.TAX).getAmount();

        AccountTransaction.Type type = pp.getType();

        if (type == AccountTransaction.Type.DEPOSIT || type == AccountTransaction.Type.REMOVAL)
        {
            if (pp.getType() == AccountTransaction.Type.REMOVAL)
                amount *= -1;

            pr.setType("Payment");
            pr.addUnit(new PRTransactionUnit("base", amount, pp.getCurrencyCode()));
        }
        else if (type == AccountTransaction.Type.INTEREST || type == AccountTransaction.Type.INTEREST_CHARGE)
        {
            if (type == AccountTransaction.Type.INTEREST_CHARGE)
                amount *= -1;

            pr.setType("DepositInterest");

            if (taxAmount != 0)
            {
                pr.addUnit(new PRTransactionUnit("tax", -1 * taxAmount, pp.getCurrencyCode()));
                amount += taxAmount;
            }

            pr.addUnit(new PRTransactionUnit("base", amount, pp.getCurrencyCode()));
        }
        else if (type == AccountTransaction.Type.FEES || type == AccountTransaction.Type.FEES_REFUND)
        {
            if (type == AccountTransaction.Type.FEES)
                amount *= -1;

            if (pp.getSecurity() == null)
            {
                pr.setType("DepositFee");
                pr.addUnit(new PRTransactionUnit("fee", amount, pp.getCurrencyCode()));
            }
            else
            {
                pr.setType("SecuritiesFee");
                pr.addUnit(new PRTransactionUnit("base", amount, pp.getCurrencyCode()));

                // Create additional transaction
                pr.setPartnerTransactionUuid(pr2.getUuid());
                ret.add(pr2);
                pr2.setType("SecuritiesFee");
                pr2.setPortfolioSecurityUuid(pp.getSecurity().getUUID());
                pr2.setAccountUuid(findPortfolioForAdditionalTransaction(pp, account).getUUID());

                pr2.addUnit(new PRTransactionUnit("fee", amount, pp.getCurrencyCode()));
            }
        }
        else if (type == AccountTransaction.Type.TAXES || type == AccountTransaction.Type.TAX_REFUND)
        {
            if (type == AccountTransaction.Type.TAXES)
                amount *= -1;

            if (pp.getSecurity() == null)
            {
                pr.setType("DepositTax");
                pr.addUnit(new PRTransactionUnit("tax", amount, pp.getCurrencyCode()));
            }
            else
            {
                pr.setType("SecuritiesTax");
                pr.addUnit(new PRTransactionUnit("base", amount, pp.getCurrencyCode()));

                // Create additional transaction
                pr.setPartnerTransactionUuid(pr2.getUuid());
                ret.add(pr2);
                pr2.setType("SecuritiesTax");
                pr2.setPortfolioSecurityUuid(pp.getSecurity().getUUID());
                pr2.setAccountUuid(findPortfolioForAdditionalTransaction(pp, account).getUUID());

                pr2.addUnit(new PRTransactionUnit("tax", amount, pp.getCurrencyCode()));
            }

        }
        else if (type == AccountTransaction.Type.DIVIDENDS)
        {
            pr.setType("SecuritiesDividend");
            pr.addUnit(new PRTransactionUnit("base", amount, pp.getCurrencyCode()));

            // Create additional transaction
            pr.setPartnerTransactionUuid(pr2.getUuid());
            ret.add(pr2);
            pr2.setType("SecuritiesDividend");
            pr2.setPortfolioSecurityUuid(pp.getSecurity().getUUID());
            pr2.setShares(pp.getShares());
            pr2.setAccountUuid(findPortfolioForAdditionalTransaction(pp, account).getUUID());

            if (feeAmount != 0)
            {
                pr2.addUnit(new PRTransactionUnit("fee", feeAmount, pp.getCurrencyCode()));
                amount += feeAmount;
            }
            if (taxAmount != 0)
            {
                pr2.addUnit(new PRTransactionUnit("tax", taxAmount, pp.getCurrencyCode()));
                amount += taxAmount;
            }

            pr2.addUnit(new PRTransactionUnit("base", amount, pp.getCurrencyCode()));
        }
        else if (type == AccountTransaction.Type.BUY || type == AccountTransaction.Type.SELL)
        {
            if (pp.getType() == AccountTransaction.Type.BUY)
                amount *= -1;

            pr.setType("SecuritiesOrder");

            pr.addUnit(new PRTransactionUnit("base", amount, pp.getCurrencyCode()));

            pr.setPartnerTransactionUuid(((BuySellEntry) pp.getCrossEntry()).getPortfolioTransaction().getUUID());
        }
        else if (type == AccountTransaction.Type.TRANSFER_IN)
        {
            pr.setType("CurrencyTransfer");
            pr.addUnit(new PRTransactionUnit("base", amount, pp.getCurrencyCode()));
            pr.setPartnerTransactionUuid(((AccountTransferEntry) pp.getCrossEntry()).getTargetTransaction().getUUID());
        }
        else if (type == AccountTransaction.Type.TRANSFER_OUT)
        {
            pr.setType("CurrencyTransfer");
            pr.addUnit(new PRTransactionUnit("base", -1 * amount, pp.getCurrencyCode()));
            pr.setPartnerTransactionUuid(((AccountTransferEntry) pp.getCrossEntry()).getSourceTransaction().getUUID());

        }

        return ret;
    }

    /**
     * Filters a list
     */
    private <T> List<T> filter(List<T> input, Predicate<T> predicate)
    {
        return input.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * Filters a list, but returns all elements if no element matches
     */
    private <T> List<T> filterWithCaution(List<T> input, Predicate<T> predicate)
    {
        List<T> filteredList = filter(input, predicate);
        return filteredList.isEmpty() ? input : filteredList;
    }

    /**
     * Finds best matching Portfolio for AccountTransaction to be used for
     * additional transaction
     */
    private Portfolio findPortfolioForAdditionalTransaction(AccountTransaction accountTransaction, Account account)
    {
        List<Portfolio> candidates = client.getPortfolios();

        // Look for portfolios with...

        // ...transactions for the linked security
        candidates = filterWithCaution(candidates,
                        p -> filter(p.getTransactions(), t -> t.getSecurity() == accountTransaction.getSecurity())
                                        .size() > 0);

        // ...transactions for the linked security before or at transaction date
        candidates = filterWithCaution(candidates,
                        p -> filter(p.getTransactions(), t -> (t.getSecurity() == accountTransaction.getSecurity()
                                        && t.getDateTime().compareTo(accountTransaction.getDateTime()) <= 0))
                                                        .size() > 0);

        // ...positive number of shares at the date of the transaction
        // TODO

        // ...right number of shares at the date of the transaction
        // (if transaction contains number of shares, e.g. dividend)
        // TODO

        // ...account as referenceAccount
        candidates = filterWithCaution(candidates, p -> p.getReferenceAccount() == account);

        return candidates.get(0);
    }

    /**
     * Saves remote transaction via API. Checks if partnerTransaction is
     * referenced and exists. If necessary updates without reference to
     * partnerTransaction and schedules retry of update.
     */
    private void saveTransaction(long portfolioId, PRTransaction transaction, List<String> remoteTransactionsUuids,
                    List<PRTransaction> retryUpdateTransactions) throws IOException
    {
        // We cannot reference non-existent partnerTransaction
        String partnerTransactionUuid = transaction.getPartnerTransactionUuid();
        if (partnerTransactionUuid != null && !remoteTransactionsUuids.contains(partnerTransactionUuid))
        {
            // Queue transaction to retry update later
            PRTransaction originalTransaction = new PRTransaction(transaction);
            retryUpdateTransactions.add(originalTransaction);

            // Remove partnerTransactionUuid
            transaction.setPartnerTransactionUuid(null);
        }

        // Create transaction
        PRTransaction createdTransaction = api.updateTransaction(portfolioId, transaction);
        remoteTransactionsUuids.add(createdTransaction.getUuid());
    }

    private void syncTransactions(long portfolioId) throws IOException
    {
        List<PRTransaction> remoteTransactions = api.listTransactions(portfolioId);

        List<String> remoteTransactionsUuids = remoteTransactions.stream().map(PRTransaction::getUuid)
                        .collect(Collectors.toList());

        Map<String, PRTransaction> unmatchedRemoteTransactionsByUuid = remoteTransactions.stream()
                        .collect(Collectors.toMap(PRTransaction::getUuid, s -> s));

        List<PRTransaction> retryUpdateTransactions = new ArrayList<>();

        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction local : portfolio.getTransactions())
            {
                PRTransaction remote = unmatchedRemoteTransactionsByUuid.remove(local.getUUID());
                PRTransaction convertedLocal = convertPortfolioTransaction(local, portfolio);

                if (remote == null)
                {
                    // TODO: Decide whether create remote or delete local

                    // Create remote
                    saveTransaction(portfolioId, convertedLocal, remoteTransactionsUuids, retryUpdateTransactions);
                }
                else
                {
                    // TODO: Decide whether update remote or local

                    // Update remote
                    saveTransaction(portfolioId, convertedLocal, remoteTransactionsUuids, retryUpdateTransactions);
                }
            }
        }

        for (Account account : client.getAccounts())
        {
            for (AccountTransaction local : account.getTransactions())
            {
                // Try to find remote partnerTransactionUuid (to be used for
                // additional transactions)
                String remotePartnerTransactionUuid;
                try
                {
                    remotePartnerTransactionUuid = unmatchedRemoteTransactionsByUuid.get(local.getUUID())
                                    .getPartnerTransactionUuid();
                }
                catch (java.lang.NullPointerException e)
                {
                    remotePartnerTransactionUuid = null;
                }

                for (PRTransaction convertedLocal : convertAccountTransaction(local, account,
                                remotePartnerTransactionUuid))
                {
                    PRTransaction remote = unmatchedRemoteTransactionsByUuid.remove(convertedLocal.getUuid());

                    if (remote == null)
                    {
                        // TODO: Decide whether create remote or delete local

                        // Create remote
                        saveTransaction(portfolioId, convertedLocal, remoteTransactionsUuids, retryUpdateTransactions);
                    }
                    else
                    {
                        // TODO: Decide whether update remote or local

                        // Update remote
                        saveTransaction(portfolioId, convertedLocal, remoteTransactionsUuids, retryUpdateTransactions);
                    }
                }
            }
        }

        // Update transactions with partnerTransactionUuid
        for (PRTransaction t : retryUpdateTransactions)
            api.updateTransaction(portfolioId, t);

        for (PRTransaction transaction : unmatchedRemoteTransactionsByUuid.values())
        {
            // TODO: Decide whether delete remote or create local

            // Delete remote
            api.deleteTransaction(portfolioId, transaction);
        }

    }
}
