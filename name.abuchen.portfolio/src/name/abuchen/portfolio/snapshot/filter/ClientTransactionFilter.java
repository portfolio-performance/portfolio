package name.abuchen.portfolio.snapshot.filter;

import java.util.Collections;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;

/**
 * Remove all transactions starting with the given transaction. Transactions are
 * sorted by exactly how the TradeCollector is sorting the transactions. It is
 * used to create a client that allows to calculate the moving average costs for
 * a given trade, i.e., the costs before the sale is applied.
 */
public class ClientTransactionFilter implements ClientFilter
{
    private final Security security;
    private final Transaction transaction;

    public ClientTransactionFilter(Security security, Transaction transaction)
    {
        this.security = security;
        this.transaction = transaction;
    }

    @Override
    public Client filter(Client client)
    {
        var txs = security.getTransactions(client);

        Collections.sort(txs, new TradeCollector.ByDateAndType());

        // find the transactions in the list
        var index = -1;
        for (int ii = 0; ii < txs.size(); ii++)
        {
            if (txs.get(ii).getTransaction().equals(transaction))
            {
                index = ii;
                break;
            }
        }

        // limit the transactions to all transactions before the given one
        if (index > 0)
            txs = txs.subList(0, index);

        ReadOnlyClient pseudoClient = new ReadOnlyClient(client);
        pseudoClient.internalAddSecurity(security);

        Account account = new Account();
        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(account);

        ReadOnlyAccount pa = new ReadOnlyAccount(account);
        pseudoClient.internalAddAccount(pa);

        ReadOnlyPortfolio pp = new ReadOnlyPortfolio(portfolio);
        pp.setReferenceAccount(pa);
        pseudoClient.internalAddPortfolio(pp);

        for (var tx : txs)
        {
            var t = tx.getTransaction();

            if (t instanceof AccountTransaction ta)
            {
                pa.internalAddTransaction(ta);
            }
            else if (t instanceof PortfolioTransaction tp //
                            && tp.getType() != PortfolioTransaction.Type.TRANSFER_IN
                            && tp.getType() != PortfolioTransaction.Type.TRANSFER_OUT)
            {
                pp.internalAddTransaction(tp);
            }
        }

        return pseudoClient;
    }

}
