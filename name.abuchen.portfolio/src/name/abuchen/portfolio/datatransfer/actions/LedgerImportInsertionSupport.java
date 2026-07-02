package name.abuchen.portfolio.datatransfer.actions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDividendTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

final class LedgerImportInsertionSupport
{
    private final Client client;

    LedgerImportInsertionSupport(Client client)
    {
        this.client = client;
    }

    void insert(AccountTransaction transaction, Account account)
    {
        if (transaction.getType() == AccountTransaction.Type.DIVIDENDS)
        {
            new LedgerDividendTransactionCreator(client).create(account, transaction.getDateTime(),
                            transaction.getAmount(), transaction.getCurrencyCode(), transaction.getSecurity(),
                            transaction.getShares(), transaction.getExDate(), null, null, transaction.getUnits().toList(),
                            transaction.getNote(), transaction.getSource());
        }
        else
        {
            new LedgerAccountOnlyTransactionCreator(client).create(account, transaction.getType(),
                            transaction.getDateTime(), transaction.getAmount(), transaction.getCurrencyCode(),
                            transaction.getSecurity(), transaction.getUnits().toList(), transaction.getNote(),
                            transaction.getSource());
        }
    }

    void insert(PortfolioTransaction transaction, Portfolio portfolio)
    {
        new LedgerDeliveryTransactionCreator(client).create(portfolio, transaction.getType(), transaction.getDateTime(),
                        transaction.getAmount(), transaction.getCurrencyCode(), transaction.getSecurity(),
                        transaction.getShares(), null, null, transaction.getUnits().toList(), transaction.getNote(),
                        transaction.getSource());
    }

    void insert(BuySellEntry entry, Account account, Portfolio portfolio)
    {
        PortfolioTransaction transaction = entry.getPortfolioTransaction();

        new LedgerBuySellTransactionCreator(client).create(portfolio, account, transaction.getType(),
                        transaction.getDateTime(), transaction.getAmount(), transaction.getCurrencyCode(),
                        transaction.getSecurity(), transaction.getShares(), transaction.getUnits().toList(),
                        transaction.getNote(), transaction.getSource());
    }

    void insert(AccountTransferEntry entry, Account source, Account target)
    {
        var sourceTransaction = entry.getSourceTransaction();
        var targetTransaction = entry.getTargetTransaction();
        var sourceForex = sourceTransaction.getUnit(Transaction.Unit.Type.GROSS_VALUE);

        new LedgerAccountTransferTransactionCreator(client).create(source, target, sourceTransaction.getDateTime(),
                        sourceTransaction.getAmount(), sourceTransaction.getCurrencyCode(), targetTransaction.getAmount(),
                        targetTransaction.getCurrencyCode(), sourceForex.map(Transaction.Unit::getForex).orElse(null),
                        sourceForex.map(Transaction.Unit::getExchangeRate).orElse(null), sourceTransaction.getNote(),
                        sourceTransaction.getSource());
    }

    void insert(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
    {
        var sourceTransaction = entry.getSourceTransaction();

        new LedgerPortfolioTransferTransactionCreator(client).create(source, target, sourceTransaction.getSecurity(),
                        sourceTransaction.getDateTime(), sourceTransaction.getShares(), sourceTransaction.getAmount(),
                        sourceTransaction.getCurrencyCode(), sourceTransaction.getNote(), sourceTransaction.getSource());
    }

    Status updateInvestmentPlanItemIfPresent(BuySellEntry entry)
    {
        DetectDuplicatesAction action = new DetectDuplicatesAction(client);
        List<Transaction> matchingInvestmentPlanTransactions = new ArrayList<>();
        PortfolioTransaction transaction = entry.getPortfolioTransaction();

        List<InvestmentPlan> plans = client.getPlans();
        var iterator = plans.stream().filter(
                        plan -> plan.getSecurity() != null && plan.getSecurity().equals(transaction.getSecurity()))
                        .iterator();

        while (iterator.hasNext())
        {
            List<Transaction> transactions = iterator.next().getTransactions(client).stream()
                            .map(pair -> (Transaction) pair.getTransaction()).toList();
            matchingInvestmentPlanTransactions.addAll(action.findInvestmentPlanTransactions(transaction, transactions));
        }

        if (matchingInvestmentPlanTransactions.size() > 1)
            return new Status(Status.Code.WARNING, Messages.LabelPotentialDuplicate);

        if (matchingInvestmentPlanTransactions.size() == 1)
        {
            updateInvestmentPlanTransaction(matchingInvestmentPlanTransactions.get(0), entry);
            return Status.OK_STATUS;
        }

        return null;
    }

    private void updateInvestmentPlanTransaction(Transaction existingTransaction, BuySellEntry importedEntry)
    {
        if (existingTransaction instanceof LedgerBackedTransaction)
        {
            updateLedgerBackedInvestmentPlanTransaction(existingTransaction, importedEntry);
            return;
        }

        PortfolioTransaction importedPortfolioTransaction = importedEntry.getPortfolioTransaction();

        existingTransaction.setDateTime(importedPortfolioTransaction.getDateTime());
        existingTransaction.setNote(importedPortfolioTransaction.getNote());
        existingTransaction.setSource(importedPortfolioTransaction.getSource());
        existingTransaction.setShares(importedPortfolioTransaction.getShares());
        existingTransaction.setAmount(importedPortfolioTransaction.getAmount());
        existingTransaction.clearUnits();
        importedPortfolioTransaction.getUnits().forEach(existingTransaction::addUnit);

        if (existingTransaction.getCrossEntry() != null)
        {
            Transaction crossTransaction = existingTransaction.getCrossEntry().getCrossTransaction(existingTransaction);
            crossTransaction.setDateTime(importedPortfolioTransaction.getDateTime());
            crossTransaction.setAmount(importedPortfolioTransaction.getAmount());
            crossTransaction.setNote(importedPortfolioTransaction.getNote());
            crossTransaction.setSource(importedPortfolioTransaction.getSource());
        }
    }

    void updateLedgerBackedInvestmentPlanTransaction(Transaction existingTransaction, BuySellEntry importedEntry)
    {
        if (!(existingTransaction instanceof PortfolioTransaction existingPortfolioTransaction))
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_UI_003.message(
                                            Messages.LedgerInsertActionInvestmentPlanLegacySettersNotSupported));

        PortfolioTransaction importedPortfolioTransaction = importedEntry.getPortfolioTransaction();

        switch (existingPortfolioTransaction.getType())
        {
            case BUY:
                updateGeneratedBuy(existingPortfolioTransaction, importedPortfolioTransaction);
                break;
            case DELIVERY_INBOUND:
                updateGeneratedInboundDelivery(existingPortfolioTransaction, importedPortfolioTransaction);
                break;
            case SELL, DELIVERY_OUTBOUND, TRANSFER_IN, TRANSFER_OUT:
                throw new UnsupportedOperationException(
                                LedgerDiagnosticCode.LEDGER_UI_004.message(MessageFormat.format(
                                                Messages.LedgerInsertActionUnsupportedInvestmentPlanTransactionUpdateType,
                                                existingPortfolioTransaction.getType())));
            default:
                throw new UnsupportedOperationException(
                                LedgerDiagnosticCode.LEDGER_UI_005.message(MessageFormat.format(
                                                Messages.LedgerInsertActionUnsupportedInvestmentPlanTransactionUpdateType,
                                                existingPortfolioTransaction.getType())));
        }
    }

    private void updateGeneratedBuy(PortfolioTransaction existingPortfolioTransaction,
                    PortfolioTransaction importedPortfolioTransaction)
    {
        if (importedPortfolioTransaction.getType() != PortfolioTransaction.Type.BUY)
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_UI_006.message(
                                            Messages.LedgerInsertActionGeneratedBuyTypeMismatch));

        if (!(existingPortfolioTransaction.getCrossEntry() instanceof BuySellEntry existingEntry))
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_UI_007
                                            .message(Messages.LedgerInsertActionGeneratedBuyMissingBuySellEntry));

        new LedgerBuySellTransactionCreator(client).update(existingEntry, existingEntry.getPortfolio(),
                        existingEntry.getAccount(), PortfolioTransaction.Type.BUY,
                        importedPortfolioTransaction.getDateTime(), importedPortfolioTransaction.getAmount(),
                        importedPortfolioTransaction.getCurrencyCode(), importedPortfolioTransaction.getSecurity(),
                        importedPortfolioTransaction.getShares(), importedPortfolioTransaction.getUnits().toList(),
                        importedPortfolioTransaction.getNote(), importedPortfolioTransaction.getSource());
    }

    private void updateGeneratedInboundDelivery(PortfolioTransaction existingPortfolioTransaction,
                    PortfolioTransaction importedPortfolioTransaction)
    {
        if (importedPortfolioTransaction.getType() != PortfolioTransaction.Type.BUY)
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_UI_008.message(
                                            Messages.LedgerInsertActionGeneratedDeliveryTypeMismatch));

        new LedgerDeliveryTransactionCreator(client).update(existingPortfolioTransaction,
                        ownerOf(existingPortfolioTransaction), PortfolioTransaction.Type.DELIVERY_INBOUND,
                        importedPortfolioTransaction.getDateTime(), importedPortfolioTransaction.getAmount(),
                        importedPortfolioTransaction.getCurrencyCode(), importedPortfolioTransaction.getSecurity(),
                        importedPortfolioTransaction.getShares(), null, null,
                        importedPortfolioTransaction.getUnits().toList(), importedPortfolioTransaction.getNote(),
                        importedPortfolioTransaction.getSource());
    }

    private Portfolio ownerOf(PortfolioTransaction transaction)
    {
        return client.getPortfolios().stream().filter(portfolio -> portfolio.getTransactions().contains(transaction))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        Messages.LedgerInsertActionGeneratedDeliveryOwnerNotFound));
    }
}
