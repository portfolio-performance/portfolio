package name.abuchen.portfolio.ui.views.actions;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;

import org.eclipse.jface.action.Action;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.LedgerAccountTransferToDepositRemovalConverter;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.ui.Messages;

public class ConvertTransferToDepositRemovalAction extends Action
{
    private final Client client;
    private final Collection<AccountTransaction> transactionList;

    public ConvertTransferToDepositRemovalAction(Client client, Collection<AccountTransaction> transactionList)
    {
        this.client = client;
        this.transactionList = transactionList;

        setText(Messages.MenuConvertToDepositRemoval);
    }

    @Override
    public void run()
    {
        var ledgerTransferCreator = new LedgerAccountTransferTransactionCreator(client);
        var ledgerSplitConverter = new LedgerAccountTransferToDepositRemovalConverter(client);
        var ledgerEntries = java.util.Collections.newSetFromMap(new IdentityHashMap<AccountTransferEntry, Boolean>());

        for (AccountTransaction transaction : transactionList)
        {
            if (!(transaction.getCrossEntry() instanceof AccountTransferEntry entry))
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_UI_018
                                .message(MessageFormat.format(
                                                Messages.LedgerConvertTransferToDepositRemovalActionUnsupportedTransferEntry,
                                                transaction)));

            if (ledgerTransferCreator.isLedgerBacked(entry))
            {
                if (!ledgerSplitConverter.canSplit(entry))
                    throw new UnsupportedOperationException(
                                    LedgerDiagnosticCode.LEDGER_UI_019
                                                    .message(Messages.LedgerConvertTransferToDepositRemovalActionCannotConvertLedgerBackedTransfer));

                ledgerEntries.add(entry);
            }
        }

        var convertedLedgerEntries = new HashSet<>(ledgerEntries);

        ledgerEntries.forEach(ledgerSplitConverter::split);

        for (AccountTransaction transaction : transactionList)
        {
            AccountTransferEntry entry = (AccountTransferEntry) transaction.getCrossEntry();

            if (ledgerTransferCreator.isLedgerBacked(entry) || convertedLedgerEntries.contains(entry))
                continue;

            Account accountFrom = entry.getSourceAccount();
            Account accountTo = entry.getTargetAccount();

            // create deposit and withdrawal transactions with same values as the
            // transfer entry
            AccountTransaction txDeposit = new AccountTransaction();
            txDeposit.setType(AccountTransaction.Type.DEPOSIT);
            AccountTransaction txRemoval = new AccountTransaction();
            txRemoval.setType(AccountTransaction.Type.REMOVAL);
            for (AccountTransaction tx : Arrays.asList(txDeposit, txRemoval))
            {
                tx.setAmount(transaction.getAmount());
                tx.setCurrencyCode(transaction.getCurrencyCode());
                tx.setDateTime(transaction.getDateTime());
                tx.setMonetaryAmount(transaction.getMonetaryAmount());
                tx.setNote(transaction.getNote());
                tx.setSecurity(transaction.getSecurity());
                tx.setShares(transaction.getShares());
            }

            // add deposit and withdrawal transactions to accounts
            accountFrom.addTransaction(txRemoval);
            accountTo.addTransaction(txDeposit);

            // delete transfer
            accountFrom.deleteTransaction(transaction, client);
            accountTo.deleteTransaction(transaction, client);
        }

        client.markDirty();
    }
}
