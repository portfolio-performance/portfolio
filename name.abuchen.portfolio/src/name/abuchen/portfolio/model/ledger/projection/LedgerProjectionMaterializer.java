package name.abuchen.portfolio.model.ledger.projection;

import java.util.Objects;
import java.util.function.Predicate;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.ledger.LedgerEntry;

/**
 * Materializes Ledger entries into account, portfolio, and cross-entry runtime views.
 * This is projection infrastructure used after creation, mutation, and load. It keeps
 * legacy owner lists in sync with Ledger truth.
 */
final class LedgerProjectionMaterializer
{
    private final LedgerProjectionFactory factory;

    LedgerProjectionMaterializer()
    {
        this(new LedgerProjectionFactory());
    }

    LedgerProjectionMaterializer(LedgerProjectionFactory factory)
    {
        this.factory = Objects.requireNonNull(factory);
    }

    void materialize(Client client)
    {
        materialize(client, entry -> true);
    }

    void materialize(Client client, Predicate<LedgerEntry> entryFilter)
    {
        Objects.requireNonNull(client);
        Objects.requireNonNull(entryFilter);

        for (var entry : client.getLedger().getEntries())
        {
            if (!entryFilter.test(entry))
                continue;

            for (var transaction : factory.createProjections(entry))
            {
                if (transaction instanceof LedgerBackedAccountTransaction accountTransaction)
                    addAccountProjection(accountTransaction);
                else if (transaction instanceof LedgerBackedPortfolioTransaction portfolioTransaction)
                    addPortfolioProjection(portfolioTransaction);
            }
        }
    }

    private void addAccountProjection(LedgerBackedAccountTransaction transaction)
    {
        var account = transaction.getLedgerProjectionRef().getAccount();

        if (account.getTransactions().stream().noneMatch(existing -> isSameLedgerProjection(existing, transaction)))
            account.getTransactions().add(transaction);
    }

    private void addPortfolioProjection(LedgerBackedPortfolioTransaction transaction)
    {
        var portfolio = transaction.getLedgerProjectionRef().getPortfolio();

        if (portfolio.getTransactions().stream().noneMatch(existing -> isSameLedgerProjection(existing, transaction)))
            portfolio.getTransactions().add(transaction);
    }

    private boolean isSameLedgerProjection(AccountTransaction existing, LedgerBackedTransaction transaction)
    {
        return existing instanceof LedgerBackedTransaction && existing.getUUID().equals(
                        transaction.getLedgerProjectionRef().getUUID());
    }

    private boolean isSameLedgerProjection(PortfolioTransaction existing, LedgerBackedTransaction transaction)
    {
        return existing instanceof LedgerBackedTransaction && existing.getUUID().equals(
                        transaction.getLedgerProjectionRef().getUUID());
    }
}
