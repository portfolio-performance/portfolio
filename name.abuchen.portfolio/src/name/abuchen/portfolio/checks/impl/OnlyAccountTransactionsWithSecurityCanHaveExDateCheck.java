package name.abuchen.portfolio.checks.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;

public class OnlyAccountTransactionsWithSecurityCanHaveExDateCheck implements Check
{
    @Override
    public List<Issue> execute(Client client)
    {
        var answer = new ArrayList<Issue>();

        for (var account : client.getAccounts())
        {
            for (var tx : List.copyOf(account.getTransactions()))
            {
                if (tx instanceof LedgerBackedAccountTransaction ledgerBacked)
                {
                    clearLedgerBackedExDate(client, ledgerBacked);
                    continue;
                }

                if (tx.getExDate() != null && tx.getSecurity() == null)
                    tx.setExDate(null);
            }
        }

        return answer;
    }

    private void clearLedgerBackedExDate(Client client, LedgerBackedAccountTransaction transaction)
    {
        var posting = LedgerProjectionSupport.primaryPosting(transaction.getLedgerEntry(),
                        transaction.getLedgerProjectionRef());

        if (posting.getSecurity() != null || !removeExDateParameters(posting))
            return;

        transaction.getLedgerEntry().setUpdatedAt(Instant.now());
        refreshLedgerEntryProjections(client, transaction.getLedgerEntry().getUUID());
    }

    private boolean removeExDateParameters(LedgerPosting posting)
    {
        var parameters = posting.getParameters().stream() //
                        .filter(parameter -> parameter.getType() == LedgerParameterType.EX_DATE) //
                        .toList();

        parameters.forEach(posting::removeParameter);

        return !parameters.isEmpty();
    }

    private void refreshLedgerEntryProjections(Client client, String entryUUID)
    {
        client.getAccounts().forEach(owner -> owner.getTransactions()
                        .removeIf(transaction -> isProjectionOfEntry(transaction, entryUUID)));
        client.getPortfolios().forEach(owner -> owner.getTransactions()
                        .removeIf(transaction -> isProjectionOfEntry(transaction, entryUUID)));

        LedgerProjectionService.materialize(client);
    }

    private boolean isProjectionOfEntry(Transaction transaction, String entryUUID)
    {
        return transaction instanceof LedgerBackedTransaction ledgerBacked
                        && ledgerBacked.getLedgerEntry().getUUID().equals(entryUUID);
    }
}
